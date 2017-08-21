package ru.runa.gpd.ui.wizard;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.SharedImages;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.settings.WFEConnectionPreferencePage;
import ru.runa.gpd.ui.custom.Dialogs;
import ru.runa.gpd.ui.custom.SyncUIHelper;
import ru.runa.gpd.ui.wizard.ImportParWizardPage.CustomWfDefinition;
import ru.runa.gpd.ui.wizard.ImportParWizardPage.CustomWfHistoryDefinition;
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
        DefinitionNode treeDefinitions = createTree(getWfDefinitionsByType(definitions));
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
                	 if (((DefinitionNode) selected).isGroup())
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

    private Map<String, List<CustomWfDefinition>> getWfDefinitionsByType(Map<WfDefinition, List<WfDefinition>> definitions) {
        Map<String, List<CustomWfDefinition>> grouppedDefinitionsMap = new HashMap<String, List<CustomWfDefinition>>();
        for (Map.Entry<WfDefinition, List<WfDefinition>> entry : definitions.entrySet()) {
            WfDefinition definition = entry.getKey();
            String[] categories = definition.getCategories();

            List<WfDefinition> historyDefinitions = entry.getValue();
            Map<String, List<CustomWfHistoryDefinition>> historyDefinitionsMap = null;
            
            removeExistedDefinitionFromHistory(definition, historyDefinitions);
            
            if(!historyDefinitions.isEmpty()){
            	historyDefinitionsMap = new HashMap<String, List<CustomWfHistoryDefinition>>();
            	List <CustomWfHistoryDefinition> customWfHistoryDefinitions = new ArrayList();
            	
            	for (WfDefinition historyDefinition : historyDefinitions) {
            		customWfHistoryDefinitions.add(new CustomWfHistoryDefinition(historyDefinition.getName(), historyDefinition.getId(), historyDefinition.getVersion()));
            	}
            	historyDefinitionsMap.put(Localization.getString("ImportParWizardPage.page.oldDefinitionVersions"), customWfHistoryDefinitions);
            }
            
            for (String category : categories) {
                if (!grouppedDefinitionsMap.containsKey(category)) {                	
                    List<CustomWfDefinition> newDefinitionlist = new ArrayList<CustomWfDefinition>();
                    newDefinitionlist.add(new CustomWfDefinition(definition.getName(), definition.getId(), historyDefinitionsMap));
                    grouppedDefinitionsMap.put(category, newDefinitionlist);                    
                } else {
                    List<CustomWfDefinition> existedDefinitionlist = (List) grouppedDefinitionsMap.get(category);
                    existedDefinitionlist.add(new CustomWfDefinition(definition.getName(), definition.getId(), historyDefinitionsMap));
                }
            }
        }
        return new TreeMap<>(grouppedDefinitionsMap);
    }
    
    private void removeExistedDefinitionFromHistory(WfDefinition definition, List<WfDefinition> historyDefinitions){
    	Long definitionVersion = definition.getVersion();
    	for (WfDefinition historyDefinition : historyDefinitions) {
    		if(definitionVersion == historyDefinition.getVersion())
    			historyDefinitions.remove(historyDefinition);
    		break;
    	}    	
    }
    
    class ViewLabelProvider extends LabelProvider {

        @Override
        public String getText(Object obj) {
        	DefinitionNode definitionNode = (DefinitionNode)obj;
            return definitionNode.isHistory() ? definitionNode.getVersion().toString() : definitionNode.getName();
        }

        @Override
        public Image getImage(Object obj) {
        	DefinitionNode definitionNode = (DefinitionNode)obj;
        	return definitionNode.isGroup ? SharedImages.getImage("icons/project.gif") : SharedImages.getImage("icons/process.gif");        	            
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
        
        @Override
        public Object getParent(Object child) {
            if (child instanceof DefinitionNode) {
                return ((DefinitionNode) child).getChildren();
            }
            return null;
        }

        @Override
        public Object[] getChildren(Object parent) {
            if (parent instanceof DefinitionNode) {
                return ((DefinitionNode) parent).getChildren().toArray();
            }
            return new Object[0];
        }

        @Override
        public boolean hasChildren(Object parent) {
            if (parent instanceof DefinitionNode) {
                return !((DefinitionNode) parent).getChildren().isEmpty();
            }
            return false;
        }
    }

    private DefinitionNode createTree(Map<String, List<CustomWfDefinition>> definitions) {
    	
    	DefinitionNode root = new DefinitionNode("Root", null, null, true, false);
    	DefinitionNode processType;
    	DefinitionNode historyProcessType;
    	
        for (Map.Entry<String, List<CustomWfDefinition>> entry : definitions.entrySet()) {
            String groupName = entry.getKey();            
            
            if(groupName.trim().isEmpty()){
            	for (CustomWfDefinition definition : entry.getValue()) {                    
            		root.addChild(new DefinitionNode(definition.getName(), definition.getId(), null, false, false));
                }
            	continue;
            }
                
            processType = new DefinitionNode(groupName, null, null, true, false);
            
            for (CustomWfDefinition definition : entry.getValue()) {
            	DefinitionNode treeObject = new DefinitionNode(definition.getName(), definition.getId(), null, false, false);
            	//add history 
            	if(null != definition.getCustomWfHistoryDefinitions() && !definition.getCustomWfHistoryDefinitions().isEmpty())
            	for (Map.Entry<String, List<CustomWfHistoryDefinition>> historyEntry : definition.getCustomWfHistoryDefinitions().entrySet()) {
                     String historyGroupName = historyEntry.getKey();
            		 historyProcessType = new DefinitionNode(historyGroupName, null, null, true, false);
                     
                     for (CustomWfHistoryDefinition historyDefinition : historyEntry.getValue()) {
                    	 historyProcessType.addChild(new DefinitionNode(historyDefinition.getName(), historyDefinition.getId(), historyDefinition.getVersion(), false, true));                    	 
                     } 
                     treeObject.addChild(historyProcessType);
            	}
                processType.addChild(treeObject);
            }
            root.addChild(processType);
        }
        return root;
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
  	    private Long version;
  	    
		public CustomWfHistoryDefinition(String name, Long id, Long version) {
			super();
			this.name = name;
			this.id = id;
			this.version = version;
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
		public Long getVersion() {
			return version;
		}
		public void setVersion(Long version) {
			this.version = version;
		}  	    
    }  
  
    class DefinitionNode extends WfDefinition{
        private List<DefinitionNode> children = new ArrayList<>();
        private DefinitionNode parent = null;
        private Long version = null;
        
        private String name = null;
        private Long id = null;
        private boolean isGroup;
        private boolean isHistory;

        public DefinitionNode(String name, Long id, Long version, boolean isGroup, boolean isHistory){
        	this.name = name;
            this.id = id;
            this.version = version;
            this.isGroup = isGroup;
            this.isHistory = isHistory;
        }
                
        public boolean isHistory() {
			return isHistory;
		}

		public void setHistory(boolean isHistory) {
			this.isHistory = isHistory;
		}

		public boolean isGroup() {
			return isGroup;
		}

		public void setGroup(boolean isGroup) {
			this.isGroup = isGroup;
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

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		public void setChildren(List<DefinitionNode> children) {
			this.children = children;
		}

        public void addChild(DefinitionNode child) {
            child.setParent(this);
            this.children.add(child);
        }

        public void addChildren(List<DefinitionNode> children) {
            for(DefinitionNode t : children) {
                t.setParent(this);
            }
            this.children.addAll(children);
        }

        public List<DefinitionNode> getChildren() {
            return children;
        }

        private void setParent(DefinitionNode parent) {
            this.parent = parent;
        }

        public DefinitionNode getParent() {
            return parent;
        }
    }
}
