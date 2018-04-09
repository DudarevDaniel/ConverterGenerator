package org.dudariev.converter.generator;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTreeUtil;


public class GenerateConverterAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiClass psiClass = getPsiClassFromContext(e);
        if (psiClass == null) {
            return;
        }
        GenerateConverterDialog generateConverterDialog = new GenerateConverterDialog(psiClass);
        generateConverterDialog.show();
        if (generateConverterDialog.isOK()) {
            ComboBox convertToComboBox = generateConverterDialog.getConvertToComboBox();
            ComboBox convertFromComboBox = generateConverterDialog.getConvertFromComboBox();
            String convertToName = (String) convertToComboBox.getEditor().getItem();
            String convertFromName = (String) convertFromComboBox.getEditor().getItem();
            PsiClass[] classesTo = PsiShortNamesCache.getInstance(psiClass.getProject()).getClassesByName(convertToName, GlobalSearchScope.projectScope(psiClass.getProject()));
            PsiClass[] classesFrom = PsiShortNamesCache.getInstance(psiClass.getProject()).getClassesByName(convertFromName, GlobalSearchScope.projectScope(psiClass.getProject()));
            PsiClass classTo = classesTo[0];
            PsiClass classFrom = classesFrom[0];
            generateConvertAs(classTo, classFrom, psiClass);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        PsiClass psiClass = getPsiClassFromContext(e);
        e.getPresentation().setEnabled(psiClass != null);
    }

    private PsiClass getPsiClassFromContext(AnActionEvent e) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (psiFile == null || editor == null) {
            return null;
        }
        int offset = editor.getCaretModel().getOffset();
        PsiElement elementAt = psiFile.findElementAt(offset);
        return PsiTreeUtil.getParentOfType(elementAt, PsiClass.class);
    }

    private void generateConvertAs(PsiClass to, PsiClass from, PsiClass psiClass) {
        new WriteCommandAction.Simple(psiClass.getProject(), psiClass.getContainingFile()) {

            @Override
            protected void run() {
                buildConvertMethod(to, from, psiClass);
            }

        }.execute();
    }

    private void buildConvertMethod(PsiClass to, PsiClass from, PsiClass psiClass) {
        StringBuilder builder = new StringBuilder("public ");
        builder.append(to.getQualifiedName());
        builder.append(" convertAs(");
        builder.append(from.getQualifiedName());
        builder.append(" from) {\n");
        builder.append(to.getQualifiedName()).append(" to = new ").append(to.getQualifiedName()).append("();\n");

        PsiField[] toFields = to.getFields();
        for (PsiField toField : toFields) {
            String toFieldName = toField.getName();
            PsiMethod toSetter = findSetter(to, toFieldName);
            PsiMethod fromGetter = findGetter(from, toFieldName);
            if (toSetter != null && fromGetter != null) {
                builder.append("to.").append(toSetter.getName()).append("(from.")
                        .append(fromGetter.getName()).append("());\n");
            }
        }
        builder.append("return to;\n}");

        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        PsiMethod convertAs = elementFactory.createMethodFromText(builder.toString(), psiClass);
        PsiElement method = psiClass.add(convertAs);
        JavaCodeStyleManager.getInstance(psiClass.getProject()).shortenClassReferences(method);
    }

    private PsiMethod findSetter(PsiClass psiClass, String fieldName) {
        PsiMethod[] setters = psiClass.findMethodsByName("set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1), false);
        if (setters.length == 1) {
            return setters[0];
        }
        return null;
    }

    private PsiMethod findGetter(PsiClass psiClass, String fieldName) {
        String methodSuffix = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        PsiMethod[] getters = psiClass.findMethodsByName("get" + methodSuffix, false);
        if (getters.length > 0) {
            return getters[0];
        }
        getters = psiClass.findMethodsByName("is" + methodSuffix, false);
        if (getters.length > 0) {
            return getters[0];
        }
        return null;
    }

}
