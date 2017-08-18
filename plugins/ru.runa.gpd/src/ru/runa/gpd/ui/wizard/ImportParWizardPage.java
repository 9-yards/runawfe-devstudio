package ru.runa.gpd.ui.wizard;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.SharedImages;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.settings.WFEConnectionPreferencePage;
import ru.runa.gpd.ui.custom.Dialogs;
import ru.runa.gpd.ui.custom.SyncUIHelper;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.wfe.ConnectorCallback;
import ru.runa.gpd.wfe.WFEServerProcessDefinitionImporter;
import ru.runa.wfe.definition.dto.WfDefinition;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class ImportParWizardPage extends ImportWizardPage {
    private Button importFromFileButton;
    private Composite fileSelectionArea;
    private Text selectedParsLabel;
    private Button selectParsButton;
    private Button importFromServerButton;
    private TreeViewer serverDefinitionViewer;
    private String selectedDirFileName;
    private String[] selectedFileNames;

    public ImportParWizardPage(String pageName, IStructuredSelection selection) {
        super(pageName, selection);
        setTitle(Localization.getString("ImportParWizardPage.page.title"));
        setDescription(Localization.getString("ImportParWizardPage.page.description"));
    }

    @Override
    public void createControl(Composite parent) {
        Composite pageControl = new Composite(parent, SWT.NONE);
        pageControl.setLayout(new GridLayout(1, false));
        pageControl.setLayoutData(new GridData(GridData.FILL_BOTH));
        SashForm sashForm = new SashForm(pageControl, SWT.HORIZONTAL);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
        createProjectsGroup(sashForm);
        Group importGroup = new Group(sashForm, SWT.NONE);
        importGroup.setLayout(new GridLayout(1, false));
        importGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        importFromFileButton = new Button(importGroup, SWT.RADIO);
        importFromFileButton.setText(Localization.getString("ImportParWizardPage.page.importFromFileButton"));
        importFromFileButton.setSelection(true);
        importFromFileButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setImportMode();
            }
        });
        fileSelectionArea = new Composite(importGroup, SWT.NONE);
        GridData fileSelectionData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        fileSelectionData.heightHint = 30;
        fileSelectionArea.setLayoutData(fileSelectionData);
        GridLayout fileSelectionLayout = new GridLayout();
        fileSelectionLayout.numColumns = 2;
        fileSelectionLayout.makeColumnsEqualWidth = false;
        fileSelectionLayout.marginWidth = 0;
        fileSelectionLayout.marginHeight = 0;
        fileSelectionArea.setLayout(fileSelectionLayout);
        selectedParsLabel = new Text(fileSelectionArea, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL);
        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        gridData.heightHint = 30;
        selectedParsLabel.setLayoutData(gridData);
        selectParsButton = new Button(fileSelectionArea, SWT.PUSH);
        selectParsButton.setText(Localization.getString("button.choose"));
        selectParsButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END));
        selectParsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
                dialog.setFilterExtensions(new String[] { "*.par" });
                if (dialog.open() != null) {
                    selectedDirFileName = dialog.getFilterPath();
                    selectedFileNames = dialog.getFileNames();
                    String text = "";
                    for (String fileName : selectedFileNames) {
                        text += fileName + "\n";
                    }
                    selectedParsLabel.setText(text);
                }
            }
        });
        importFromServerButton = new Button(importGroup, SWT.RADIO);
        importFromServerButton.setText(Localization.getString("ImportParWizardPage.page.importFromServerButton"));
        SyncUIHelper.createHeader(importGroup, WFEServerProcessDefinitionImporter.getInstance(), WFEConnectionPreferencePage.class,
                new ConnectorCallback() {

                    @Override
                    public void onSynchronizationFailed(Exception e) {
                        Dialogs.error(Localization.getString("error.Synchronize"), e);
                    }

                    @Override
                    public void onSynchronizationCompleted() {
                    	setupServerDefinitionViewer();
                    }
                });
        createServerDefinitionsGroup(importGroup);
        setControl(pageControl);
    }

    private void setImportMode() {
        boolean fromFile = importFromFileButton.getSelection();
        selectParsButton.setEnabled(fromFile);
        if (fromFile) {
            serverDefinitionViewer.setInput(new Object());
        } else {
            if (WFEServerProcessDefinitionImporter.getInstance().isConfigured()) {
                if (!WFEServerProcessDefinitionImporter.getInstance().hasCachedData()) {
                    long start = System.currentTimeMillis();
                    WFEServerProcessDefinitionImporter.getInstance().synchronize();
                    long end = System.currentTimeMillis();
                    PluginLogger.logInfo("def sync [sec]: " + ((end - start) / 1000));
                }
                setupServerDefinitionViewer();
            }
        }
    }

    private void setupServerDefinitionViewer(){
    	Map<WfDefinition, List<WfDefinition>> definitions = WFEServerProcessDefinitionImporter.getInstance().loadCachedData();
        //TreeObject treeDefinitions = createTree(getWfDefinitionsByType(definitions));
    	MyTreeNode treeDefinitions = createTree(getWfDefinitionsByType(definitions));
        serverDefinitionViewer.setInput(treeDefinitions);
        serverDefinitionViewer.refresh(true);
    }
    
    private void createServerDefinitionsGroup(Composite parent) {
        serverDefinitionViewer = new TreeViewer(parent);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 100;
        serverDefinitionViewer.getControl().setLayoutData(gridData);
        serverDefinitionViewer.setContentProvider(new ViewContentProvider());
        serverDefinitionViewer.setLabelProvider(new ViewLabelProvider());
        serverDefinitionViewer.setInput(new Object());
    }

    public boolean performFinish() {    	
        InputStream[] parInputStreams = null;
        try {
            IContainer container = getSelectedContainer();
            String[] processNames;
            boolean fromFile = importFromFileButton.getSelection();
            if (fromFile) {
                if (selectedDirFileName == null) {
                    throw new Exception(Localization.getString("ImportParWizardPage.error.selectValidPar"));
                }
                processNames = new String[selectedFileNames.length];
                parInputStreams = new InputStream[selectedFileNames.length];
                for (int i = 0; i < selectedFileNames.length; i++) {
                    processNames[i] = selectedFileNames[i].substring(0, selectedFileNames[i].length() - 4);
                    String fileName = selectedDirFileName + File.separator + selectedFileNames[i];
                    parInputStreams[i] = new FileInputStream(fileName);
                }
            } else {
            	TreeItem[] selections = serverDefinitionViewer.getTree().getSelection();
            	List<WfDefinition> defSelections = Lists.newArrayList();
                for(int i = 0; i < selections.length; i++){
                	Object selected = selections[i].getData();
                	if (MyTreeNode.class.isInstance(selected))
                		continue;
                    if (selected instanceof WfDefinition) {
                        defSelections.add((WfDefinition) selected);
                    }
                }                
                if (defSelections.isEmpty()) {
                    throw new Exception(Localization.getString("ImportParWizardPage.error.selectValidDefinition"));
                }
                processNames = new String[defSelections.size()];
                parInputStreams = new InputStream[defSelections.size()];
                for (int i = 0; i < processNames.length; i++) {
                    WfDefinition stub = defSelections.get(i);
                    processNames[i] = stub.getName();
                    byte[] par = WFEServerProcessDefinitionImporter.getInstance().loadPar(stub);
                    parInputStreams[i] = new ByteArrayInputStream(par);
                }
            }
            for (int i = 0; i < processNames.length; i++) {
                String processName = processNames[i];
                IFolder processFolder = IOUtils.getProcessFolder(container, processName);
                if (processFolder.exists()) {
                    throw new Exception(Localization.getString("ImportParWizardPage.error.processWithSameNameExists"));
                }
                processFolder.create(true, true, null);
                IOUtils.extractArchiveToFolder(parInputStreams[i], processFolder);
                IFile definitionFile = IOUtils.getProcessDefinitionFile(processFolder);
                ProcessDefinition definition = ProcessCache.newProcessDefinitionWasCreated(definitionFile);
                if (definition != null && !Objects.equal(definition.getName(), processFolder.getName())) {
                    // if par name differs from definition name
                    IPath destination = IOUtils.getProcessFolder(container, definition.getName()).getFullPath();
                    processFolder.move(destination, true, false, null);
                    processFolder = IOUtils.getProcessFolder(container, definition.getName());
                    IFile movedDefinitionFile = IOUtils.getProcessDefinitionFile(processFolder);
                    ProcessCache.newProcessDefinitionWasCreated(movedDefinitionFile);
                    ProcessCache.invalidateProcessDefinition(definitionFile);
                }
            }
        } catch (Exception exception) {
            PluginLogger.logErrorWithoutDialog("import par", exception);
            setErrorMessage(Throwables.getRootCause(exception).getMessage());
            return false;
        } finally {
            if (parInputStreams != null) {
                for (InputStream inputStream : parInputStreams) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        return true;
    }

    public static class DefinitionTreeContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof HistoryRoot) {
                HistoryRoot historyRoot = (HistoryRoot) parentElement;
                List<WfDefinition> history = WFEServerProcessDefinitionImporter.getInstance().loadCachedData().get(historyRoot.definition);
                List<WfDefinition> result = Lists.newArrayList(history);
                result.remove(0);
                return result.toArray();
            }
            if (WFEServerProcessDefinitionImporter.getInstance().loadCachedData().containsKey(parentElement)) {
                return new Object[] { new HistoryRoot((WfDefinition) parentElement) };
            }
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            if (element instanceof HistoryRoot) {
                return true;
            }
            List<WfDefinition> history = WFEServerProcessDefinitionImporter.getInstance().loadCachedData().get(element);
            return (history != null && history.size() > 1);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof Map) {
                ArrayList<WfDefinition> arrayList = new ArrayList<WfDefinition>();
                arrayList.addAll(((Map<WfDefinition, List<WfDefinition>>) inputElement).keySet());
                Collections.sort(arrayList);
                return arrayList.toArray(new WfDefinition[arrayList.size()]);
            }
            return new Object[0];
        }

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    public static class HistoryRoot {
        private final WfDefinition definition;

        public HistoryRoot(WfDefinition stub) {
            this.definition = stub;
        }
    }

    public static class DefinitionLabelProvider extends LabelProvider {
        @Override
        public String getText(Object element) {
            if (element instanceof WfDefinition) {
                WfDefinition definition = (WfDefinition) element;
                if (WFEServerProcessDefinitionImporter.getInstance().loadCachedData().containsKey(definition)) {
                    return definition.getName();
                }
                return String.valueOf(definition.getVersion());
            }
            if (element instanceof HistoryRoot) {
                return Localization.getString("ImportParWizardPage.page.oldDefinitionVersions");
            }
            return super.getText(element);
        }
    }
    /*
    private Map<String, List<WfDefinition>> getWfDefinitionsByType(Map<WfDefinition, List<WfDefinition>> definitions) {
        Map grouppedDefinitionsMap = new HashMap<String, List<WfDefinition>>();
        for (Map.Entry<WfDefinition, List<WfDefinition>> entry : definitions.entrySet()) {
            WfDefinition definition = entry.getKey();
            String[] categories = definition.getCategories();

            for (String category : categories) {
                if (!grouppedDefinitionsMap.containsKey(category)) {
                    List<WfDefinition> newDefinitionlist = new ArrayList<WfDefinition>();
                    newDefinitionlist.add(definition);
                    grouppedDefinitionsMap.put(category, newDefinitionlist);
                } else {
                    List existedDefinitionlist = (List) grouppedDefinitionsMap.get(category);
                    existedDefinitionlist.add(definition);
                }
            }
        }
        return new TreeMap<>(grouppedDefinitionsMap);
    }*/

    /*new good 
    private Map<String, List<CustomWfDefinition>> getWfDefinitionsByType(Map<WfDefinition, List<WfDefinition>> definitions) {
        Map grouppedDefinitionsMap = new HashMap<String, List<CustomWfDefinition>>();
        for (Map.Entry<WfDefinition, List<WfDefinition>> entry : definitions.entrySet()) {
            WfDefinition definition = entry.getKey();
            String[] categories = definition.getCategories();

            for (String category : categories) {
                if (!grouppedDefinitionsMap.containsKey(category)) {
                    List<CustomWfDefinition> newDefinitionlist = new ArrayList<CustomWfDefinition>();
                    newDefinitionlist.add(new CustomWfDefinition(definition.getName(), definition.getId(), null));
                    grouppedDefinitionsMap.put(category, newDefinitionlist);
                } else {
                    List existedDefinitionlist = (List) grouppedDefinitionsMap.get(category);
                    existedDefinitionlist.add(new CustomWfDefinition(definition.getName(), definition.getId(), null));
                }
            }
        }
        return new TreeMap<>(grouppedDefinitionsMap);
    }*/

    private Map<String, List<CustomWfDefinition>> getWfDefinitionsByType(Map<WfDefinition, List<WfDefinition>> definitions) {
        Map grouppedDefinitionsMap = new HashMap<String, List<CustomWfDefinition>>();
        for (Map.Entry<WfDefinition, List<WfDefinition>> entry : definitions.entrySet()) {
            WfDefinition definition = entry.getKey();
            String[] categories = definition.getCategories();

            List<WfDefinition> historyDefinitions = entry.getValue();
            Map historyDefinitionsMap = null;
            if(!historyDefinitions.isEmpty()){
            	historyDefinitionsMap = new HashMap<String, List<CustomWfHistoryDefinition>>();
            	List <CustomWfHistoryDefinition> customWfHistoryDefinitions = new ArrayList();
            	
            	for (WfDefinition historyDefinition : historyDefinitions) {
            		customWfHistoryDefinitions.add(new CustomWfHistoryDefinition(historyDefinition.getName(), historyDefinition.getId()));
            	}
            	historyDefinitionsMap.put("история", customWfHistoryDefinitions);
            }
            
            for (String category : categories) {
                if (!grouppedDefinitionsMap.containsKey(category)) {
                	
                    List<CustomWfDefinition> newDefinitionlist = new ArrayList<CustomWfDefinition>();
                    newDefinitionlist.add(new CustomWfDefinition(definition.getName(), definition.getId(), historyDefinitionsMap));
                    grouppedDefinitionsMap.put(category, newDefinitionlist);
                    
                } else {
                    List existedDefinitionlist = (List) grouppedDefinitionsMap.get(category);
                    existedDefinitionlist.add(new CustomWfDefinition(definition.getName(), definition.getId(), historyDefinitionsMap));
                }
            }
        }
        return new TreeMap<>(grouppedDefinitionsMap);
    }
    
    class ViewLabelProvider extends LabelProvider {

        @Override
        public String getText(Object obj) {
            return obj.toString();
        }

        @Override
        public Image getImage(Object obj) {
        	/*if (obj instanceof ProcessType) {
                return SharedImages.getImage("icons/project.gif");
            }*/
            return SharedImages.getImage("icons/process.gif");            
        }
    }

    class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public Object[] getElements(Object parent) {
            return getChildren(parent);
        }
/*
        @Override
        public Object getParent(Object child) {
            if (child instanceof TreeObject) {
                return ((TreeObject) child).getParent();
            }
            return null;
        }

        @Override
        public Object[] getChildren(Object parent) {
            if (parent instanceof ProcessType) {
                return ((ProcessType) parent).getChildren();
            }
            return new Object[0];
        }

        @Override
        public boolean hasChildren(Object parent) {
            if (parent instanceof ProcessType) {
                return ((ProcessType) parent).hasChildren();
            }
            return false;
        }*/
        
        @Override
        public Object getParent(Object child) {
            if (child instanceof MyTreeNode) {
                return ((MyTreeNode) child).getChildren();
            }
            return null;
        }

        @Override
        public Object[] getChildren(Object parent) {
            if (parent instanceof MyTreeNode) {
                return ((MyTreeNode) parent).getChildren().toArray();
            }
            return new Object[0];
        }

        @Override
        public boolean hasChildren(Object parent) {
            if (parent instanceof MyTreeNode) {
                return !((MyTreeNode) parent).getChildren().isEmpty();
            }
            return false;
        }
    }
/*
    class TreeObject extends WfDefinition {
        private final String name;
        private ProcessType processType;
        private Long id;

        public TreeObject(Long id, String name) {
            this.name = name;
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setParent(ProcessType processType) {
            this.processType = processType;
        }

        public ProcessType getParent() {
            return processType;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    class ProcessType extends TreeObject {
        private final ArrayList children;

        public ProcessType(String name) {
            super(null, name);
            children = new ArrayList();
        }

        public void addChild(TreeObject child) {
            children.add(child);
            child.setParent(this);
        }

        public void removeChild(TreeObject child) {
            children.remove(child);
            child.setParent(null);
        }

        public WfDefinition[] getChildren() {
            return (WfDefinition[]) children.toArray(new TreeObject[children.size()]);
        }

        public boolean hasChildren() {
            return children.size() > 0;
        }
    }
    */
    
    /* good
    private TreeObject createTree(Map<String, List<CustomWfDefinition>> definitions) {
        ProcessType root = new ProcessType("");
        ProcessType processType;
        for (Map.Entry<String, List<CustomWfDefinition>> entry : definitions.entrySet()) {
            String groupName = entry.getKey();
            
            if(groupName.trim().isEmpty()){
            	for (CustomWfDefinition definition : entry.getValue()) {
                    root.addChild(new TreeObject(definition.getId(), definition.getName()));
                }
            	continue;
            }
                
            processType = new ProcessType(groupName);
            for (CustomWfDefinition definition : entry.getValue()) {
                processType.addChild(new TreeObject(definition.getId(), definition.getName()));
            }
            root.addChild(processType);
        }
        return root;
    }*/
    
    /* second good
    private TreeObject createTree(Map<String, List<CustomWfDefinition>> definitions) {
        ProcessType root = new ProcessType("");
        ProcessType processType;
        ProcessType historyProcessType;
        for (Map.Entry<String, List<CustomWfDefinition>> entry : definitions.entrySet()) {
            String groupName = entry.getKey();
            
            if(groupName.trim().isEmpty()){
            	for (CustomWfDefinition definition : entry.getValue()) {
                    root.addChild(new TreeObject(definition.getId(), definition.getName()));
                }
            	continue;
            }
                
            processType = new ProcessType(groupName);
            for (CustomWfDefinition definition : entry.getValue()) {
            	TreeObject treeObject = new TreeObject(definition.getId(), definition.getName());
            	 
            	for (Map.Entry<String, List<CustomWfHistoryDefinition>> historyEntry : definition.getCustomWfHistoryDefinitions().entrySet()) {
                     String historyGroupName = historyEntry.getKey();
                     
                     historyProcessType = new ProcessType(historyGroupName);
                     
                     for (CustomWfHistoryDefinition historyDefinition : historyEntry.getValue()) {
                    	 historyProcessType.addChild(new TreeObject(historyDefinition.getId(), historyDefinition.getName()));                    	 
                     }
                     
                     treeObject.setParent(historyProcessType);                     
            	}
            	
            	
            	
                processType.addChild(treeObject);
            }
            root.addChild(processType);
        }
        return root;
    }
    */

    private MyTreeNode<String> createTree(Map<String, List<CustomWfDefinition>> definitions) {
    	MyTreeNode<String> root = new MyTreeNode<>("Root");

    	MyTreeNode<String> child1 = new MyTreeNode<>("Child1");
    	child1.addChild("Grandchild1");
    	child1.addChild("Grandchild2");

    	MyTreeNode<String> child2 = new MyTreeNode<>("Child2");
    	child2.addChild("Grandchild3");

    	root.addChild(child1);
    	root.addChild(child2);
    	root.addChild("Child3");
/*
    	root.addChildren(Arrays.asList(
    	        new MyTreeNode<>("Child4"),
    	        new MyTreeNode<>("Child5"),
    	        new MyTreeNode<>("Child6")
    	));*/
    	 
    	 return root;

    	/*
    	Node root = new Node("");
    	Node processType;
    	Node historyProcessType;
        for (Map.Entry<String, List<CustomWfDefinition>> entry : definitions.entrySet()) {
            String groupName = entry.getKey();
            
            /*
            if(groupName.trim().isEmpty()){
            	for (CustomWfDefinition definition : entry.getValue()) {
                    
            		root.addChild(new Node(definition.getId(), definition.getName()));
                }
            	continue;
            }*/
 /*               
            processType = new Node(groupName);
            for (CustomWfDefinition definition : entry.getValue()) {
            	Node treeObject = new Node(definition.getId(), definition.getName(), null);
            	 
            	for (Map.Entry<String, List<CustomWfHistoryDefinition>> historyEntry : definition.getCustomWfHistoryDefinitions().entrySet()) {
                     String historyGroupName = historyEntry.getKey();
                     
                     historyProcessType = new Node(historyGroupName);
                     
                     for (CustomWfHistoryDefinition historyDefinition : historyEntry.getValue()) {
                    	 historyProcessType.addChild(new Node(historyDefinition.getId(), historyDefinition.getName(), null));                    	 
                     }
                     
                     treeObject.setParent(historyProcessType);                     
            	}
            	
            	
            	
                processType.addChild(treeObject);
            }
            root.addChild(processType);
        }*/
        //return parentNode;
    }
    
    class CustomWfDefinition{
    	private String name;
  	    private Long id; 
  	    private Map<String, List<CustomWfHistoryDefinition>> customWfHistoryDefinitions;
		
  	    public CustomWfDefinition(String name, Long id,
  	    		Map<String, List<CustomWfHistoryDefinition>> customWfHistoryDefinitions) {
			super();
			this.name = name;
			this.id = id;
			this.customWfHistoryDefinitions = customWfHistoryDefinitions;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Map<String, List<CustomWfHistoryDefinition>> getCustomWfHistoryDefinitions() {
			return customWfHistoryDefinitions;
		}

		public void setCustomWfHistoryDefinitions(
				Map<String, List<CustomWfHistoryDefinition>> customWfHistoryDefinitions) {
			this.customWfHistoryDefinitions = customWfHistoryDefinitions;
		}		
    }
    
    class CustomWfHistoryDefinition{
    	private String name;
  	    private Long id;  	    
  	    
		public CustomWfHistoryDefinition(String name, Long id) {
			super();
			this.name = name;
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}  	    
    }  
    
/*
    private TreeObject createTree(Map<String, List<WfDefinition>> definitions) {
        ProcessType root = new ProcessType("");
        ProcessType processType;
        for (Map.Entry<String, List<WfDefinition>> entry : definitions.entrySet()) {
            String groupName = entry.getKey();
            
            if(groupName.trim().isEmpty()){
            	for (WfDefinition definition : entry.getValue()) {
                    root.addChild(new TreeObject(definition.getId(), definition.getName()));
                }
            	continue;
            }
                
            processType = new ProcessType(groupName);
            for (WfDefinition definition : entry.getValue()) {
                processType.addChild(new TreeObject(definition.getId(), definition.getName()));
            }
            root.addChild(processType);
        }
        return root;
    }*/
    
    public class MyTreeNode<T>{
        private T data = null;
        private List<MyTreeNode> children = new ArrayList<>();
        private MyTreeNode parent = null;

        public MyTreeNode(T data) {
            this.data = data;
        }

        public void addChild(MyTreeNode child) {
            child.setParent(this);
            this.children.add(child);
        }

        public void addChild(T data) {
            MyTreeNode<T> newChild = new MyTreeNode<>(data);
            newChild.setParent(this);
            children.add(newChild);
        }

        public void addChildren(List<MyTreeNode> children) {
            for(MyTreeNode t : children) {
                t.setParent(this);
            }
            this.children.addAll(children);
        }

        public List<MyTreeNode> getChildren() {
            return children;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        private void setParent(MyTreeNode parent) {
            this.parent = parent;
        }

        public MyTreeNode getParent() {
            return parent;
        }
    }
}

/*
 * Node<String> parentNode = new Node<String>("Parent"); 
Node<String> childNode1 = new Node<String>("Child 1", parentNode);
Node<String> childNode2 = new Node<String>("Child 2");     

childNode2.setParent(parentNode); 

Node<String> grandchildNode = new Node<String>("Grandchild of parentNode. Child of childNode1", childNode1); 
List<Node<String>> childrenNodes = parentNode.getChildren();*/
