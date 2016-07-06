package ru.runa.gpd.ltk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.ui.refactoring.TextEditChangeNode;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import ru.runa.gpd.Localization;
import ru.runa.gpd.form.FormType;
import ru.runa.gpd.form.FormTypeProvider;
import ru.runa.gpd.lang.model.FormNode;
import ru.runa.gpd.lang.model.Variable;

import com.google.common.base.Objects;

public class FormNodePresentation extends VariableRenameProvider<FormNode> {
    private final IFolder folder;

    public FormNodePresentation(IFolder folder, FormNode formNode) {
        this.folder = folder;
        setElement(formNode);
    }

    @Override
    public List<Change> getChanges(SortedMap<Variable, Variable> variablesMap) throws Exception {
        CompositeChange result = new CompositeChange(element.getName());
        if (element.hasForm()) {
            FormType formType = FormTypeProvider.getFormType(element.getFormType());
            IFile fileForm = folder.getFile(element.getFormFileName());
            IFile fileValidation = folder.getFile(element.getValidationFileName());
            String formLabel = Localization.getString("Search.formNode.form");
            String validationLabel = Localization.getString("Search.formNode.validation");
            result.addAll(processFile(formType, fileForm, formLabel, variablesMap, false));
            if (element.hasFormValidation()) {
                result.addAll(processFile(formType, fileValidation, validationLabel, variablesMap, true));
            }
        }
        if (result.getChildren().length > 0) {
            return Arrays.asList((Change) result);
        }
        return new ArrayList<Change>();
    }

    private Change[] processFile(FormType formType, IFile file, final String label, Map<Variable, Variable> variablesMap, boolean checkScriptingName)
            throws Exception {
        List<Change> changes = new ArrayList<Change>();
        MultiTextEdit multiEditResult = new MultiTextEdit();
        for (Entry<Variable, Variable> entry : variablesMap.entrySet()) {
            Variable oldVariable = entry.getKey();
            Variable newVariable = entry.getValue();
            addChildEdit(multiEditResult, formType.searchVariableReplacements(file, oldVariable.getName(), newVariable.getName()));
            if (checkScriptingName && !Objects.equal(oldVariable.getName(), oldVariable.getScriptingName())) {
                addChildEdit(multiEditResult,
                        formType.searchVariableReplacements(file, oldVariable.getScriptingName(), newVariable.getScriptingName()));
            }
        }
        if (multiEditResult.getChildrenSize() > 0) {
            TextFileChange fileChange = new TextFileChange(file.getName(), file) {
                @SuppressWarnings("rawtypes")
                @Override
                public Object getAdapter(Class adapter) {
                    if (adapter == TextEditChangeNode.class) {
                        return new ChangeNode(this, element, label);
                    }
                    return super.getAdapter(adapter);
                }
            };
            fileChange.setEdit(multiEditResult);
            changes.add(fileChange);
        }
        return changes.toArray(new Change[changes.size()]);
    }

    public static void addChildEdit(MultiTextEdit multiTextEdit1, TextEdit textEdit2) {
        TextEdit[] children = null;
        if (textEdit2 instanceof MultiTextEdit) {
            if (textEdit2.hasChildren()) {
                children = textEdit2.removeChildren();
            }
        } else if (textEdit2 instanceof ReplaceEdit) {
            children = new TextEdit[] { textEdit2 };
        }
        if (children != null) {
            for (TextEdit child : children) {
                boolean addChild = true;
                for (TextEdit textEdit : multiTextEdit1.getChildren()) {
                    if (textEdit.getOffset() == child.getOffset()) {
                        if (child.getLength() > textEdit.getLength()) {
                            multiTextEdit1.removeChild(textEdit);
                        } else {
                            addChild = false;
                            break;
                        }
                    }
                }
                if (addChild) {
                    multiTextEdit1.addChild(child);
                }
            }
        }
    }
}
