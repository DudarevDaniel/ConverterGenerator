package org.dudariev.converter.generator;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


public class GenerateConverterDialog extends DialogWrapper {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 100;

    private JPanel dialog;
    private PsiClass psiClass;
    private TextFieldWithAutoCompletion<String> toField;
    private TextFieldWithAutoCompletion<String> fromField;

    public GenerateConverterDialog(PsiClass psiClass) {
        super(psiClass.getProject());
        this.psiClass = psiClass;
        this.dialog = createConverterDialog();
        List<String> classNamesForAutocompletion = getClassNamesForAutocompletion();
        this.toField = createTextField(classNamesForAutocompletion);
        this.fromField = createTextField(classNamesForAutocompletion);

        LabeledComponent<TextFieldWithAutoCompletion> convertToComponent = LabeledComponent.create(toField, "Convert To class");
        LabeledComponent<TextFieldWithAutoCompletion> convertFromComponent = LabeledComponent.create(fromField, "Convert From class");

        dialog.add(convertToComponent, BorderLayout.PAGE_START);
        dialog.add(convertFromComponent, BorderLayout.PAGE_END);

        init();
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        ValidationInfo toFieldValidation = validateTextField(toField, "Target");
        if (toFieldValidation == null) {
            return validateTextField(fromField, "From");
        }
        return toFieldValidation;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return dialog;
    }

    private JPanel createConverterDialog() {
        setTitle("Select Classes for Conversion");
        JPanel dialog = new JPanel(new BorderLayout());
        dialog.setPreferredSize(JBUI.size(WIDTH, HEIGHT));
        dialog.setMinimumSize(JBUI.size(WIDTH, HEIGHT));
        dialog.setMaximumSize(JBUI.size(WIDTH + 100, HEIGHT + 20));
        return dialog;
    }

    private List<String> getClassNamesForAutocompletion() {
        List<String> history = Stream.of(EditorHistoryManager.getInstance(psiClass.getProject()).getFiles())
                .map(VirtualFile::getNameWithoutExtension)
                .distinct()
                .collect(toList());

        List<String> projectFiles = FileBasedIndex.getInstance()
                .getContainingFiles(
                        FileTypeIndex.NAME,
                        JavaFileType.INSTANCE,
                        GlobalSearchScope.allScope(psiClass.getProject())
                ).stream()
                .map(VirtualFile::getNameWithoutExtension)
                .collect(toList());

        history.addAll(projectFiles);
        return history;
    }

    private TextFieldWithAutoCompletion<String> createTextField(List<String> classNames) {
        TextFieldWithAutoCompletion<String> textField = TextFieldWithAutoCompletion.create(psiClass.getProject(), classNames, true, null);
        textField.setOneLineMode(true);
        return textField;
    }

    public PsiClass getConvertToClass() {
        return extractPsiClass(this.toField);
    }

    public PsiClass getConvertFromClass() {
        return extractPsiClass(this.fromField);
    }

    private PsiClass extractPsiClass(TextFieldWithAutoCompletion<String> textField) {
        String className = textField.getText();
        if (className.isEmpty()) {
            throw new IllegalArgumentException("Should select smth");
        }
        PsiClass[] resolvedClasses = PsiShortNamesCache.getInstance(psiClass.getProject()).getClassesByName(className, GlobalSearchScope.projectScope(psiClass.getProject()));
        if (resolvedClasses.length == 0) {
            throw new IllegalArgumentException("No such class found: " + className);
        }
        return resolvedClasses[0];
    }

    private ValidationInfo validateTextField(TextFieldWithAutoCompletion<String> textField, String fieldName) {
        String className = textField.getText();
        if (className.isEmpty()) {
            return new ValidationInfo(String.format("%s class should be selected", fieldName), textField);
        }
        PsiClass[] resolvedClasses = PsiShortNamesCache.getInstance(psiClass.getProject()).getClassesByName(className, GlobalSearchScope.projectScope(psiClass.getProject()));
        if (resolvedClasses.length == 0) {
            return new ValidationInfo(String.format("Failed to find a class %s in the current project", className), textField);
        }
        return null;
    }
}
