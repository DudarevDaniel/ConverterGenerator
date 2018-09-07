package org.dudariev.converter.generator;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;


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
            PsiClass classTo = generateConverterDialog.getConvertToClass();
            PsiClass classFrom = generateConverterDialog.getConvertFromClass();
            generateConvertAs(classTo, classFrom, psiClass, generateConverterDialog.isInheritFields());
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

    private void generateConvertAs(PsiClass to, PsiClass from, PsiClass psiClass, boolean useInherited) {
        new WriteCommandAction.Simple(psiClass.getProject(), psiClass.getContainingFile()) {

            @Override
            protected void run() {
                buildConvertMethod(to, from, psiClass, useInherited);
            }

        }.execute();
    }

    private void buildConvertMethod(PsiClass to, PsiClass from, PsiClass psiClass, boolean useInherited) {
        StringBuilder builder = buildMethodSignature(to, from);

        FieldsMappingResult mappingResult = new FieldsMappingResult();
        processToFields(to, from, mappingResult, useInherited);
        processFromFields(from, mappingResult, useInherited);

        String indentation = getProjectIndentation(psiClass);
        builder.append(writeMappedFields(mappingResult));
        builder.append(writeNotMappedFields(mappingResult.getNotMappedToFields(), indentation, "TO"));
        builder.append(writeNotMappedFields(mappingResult.getNotMappedFromFields(), indentation, "FROM"));

        builder.append("return to;\n}");

        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
        PsiMethod convertAs = elementFactory.createMethodFromText(builder.toString(), psiClass);
        PsiElement method = psiClass.add(convertAs);
        JavaCodeStyleManager.getInstance(psiClass.getProject()).shortenClassReferences(method);
    }

    private void processToFields(PsiClass to, PsiClass from, FieldsMappingResult mappingResult, boolean useInherited) {
        for (PsiField toField : getFields(to, useInherited)) {
            String toFieldName = toField.getName();
            if (toFieldName != null && !toField.hasModifier(JvmModifier.STATIC)) {
                PsiMethod toSetter = findSetter(to, toFieldName, useInherited);
                PsiMethod fromGetter = findGetter(from, toFieldName, useInherited);
                if (toSetter != null && fromGetter != null && isMatchingFieldType(toField, fromGetter)) {
                    mappingResult.addMappedField(toSetter, fromGetter);
                } else {
                    mappingResult.addNotMappedToField(toFieldName);
                }
            }
        }
    }

    private void processFromFields(PsiClass from, FieldsMappingResult mappingResult, boolean useInherited) {
        for (PsiField fromField : getFields(from, useInherited)) {
            String fromFieldName = fromField.getName();
            if (fromFieldName != null && !fromField.hasModifier(JvmModifier.STATIC)) {
                PsiMethod fromGetter = findGetter(from, fromFieldName, useInherited);
                if (fromGetter == null || !mappingResult.getMappedFields().containsValue(fromGetter)) {
                    mappingResult.addNotMappedFromField(fromFieldName);
                }
            }
        }
    }

    @NotNull
    private PsiField[] getFields(PsiClass clazz, boolean useInherited) {
        PsiField[] fields;
        if (useInherited) {
            fields = clazz.getAllFields();
        } else {
            fields = clazz.getFields();
        }
        return fields;
    }

    @NotNull
    private StringBuilder buildMethodSignature(PsiClass to, PsiClass from) {
        StringBuilder builder = new StringBuilder("public ");
        builder.append(to.getQualifiedName());
        builder.append(" convertAs(");
        builder.append(from.getQualifiedName());
        builder.append(" from) {\n");
        builder.append(to.getQualifiedName()).append(" to = new ").append(to.getQualifiedName()).append("();\n");
        return builder;
    }

    @NotNull
    private String writeMappedFields(FieldsMappingResult mappingResult) {
        StringBuilder builder = new StringBuilder();
        for (PsiMethod toSetter : mappingResult.getMappedFields().keySet()) {
            builder.append("to.").append(toSetter.getName()).append("(from.")
                    .append(mappingResult.getMappedFields().get(toSetter).getName()).append("());\n");
        }
        return builder.toString();
    }

    @NotNull
    private String writeNotMappedFields(List<String> notMappedFields, String indentation, String sourceType) {
        StringBuilder builder = new StringBuilder();
        if (!notMappedFields.isEmpty()) {
            builder.append("\n").append(indentation).append("// Not mapped ").append(sourceType).append(" fields: \n");
        }
        for (String notMappedField : notMappedFields) {
            builder.append(indentation).append("// ").append(notMappedField).append("\n");
        }
        return builder.toString();
    }

    private PsiMethod findSetter(PsiClass psiClass, String fieldName, boolean useInherited) {
        PsiMethod[] setters = psiClass.findMethodsByName("set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1), useInherited);
        if (setters.length == 1) {
            return setters[0];
        }
        return null;
    }

    private PsiMethod findGetter(PsiClass psiClass, String fieldName, boolean useInherited) {
        String methodSuffix = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        PsiMethod[] getters = psiClass.findMethodsByName("get" + methodSuffix, useInherited);
        if (getters.length > 0) {
            return getters[0];
        }
        getters = psiClass.findMethodsByName("is" + methodSuffix, false);
        if (getters.length > 0) {
            return getters[0];
        }
        return null;
    }

    private String getProjectIndentation(PsiClass psiClass) {
        CommonCodeStyleSettings.IndentOptions indentOptions = CodeStyleSettings.IndentOptions.retrieveFromAssociatedDocument(psiClass.getContainingFile());
        String indentation = "        ";
        if (indentOptions != null) {
            if (indentOptions.USE_TAB_CHARACTER) {
                indentation = "\t\t";
            } else {
                indentation = new String(new char[2 * indentOptions.INDENT_SIZE]).replace("\0", " ");
            }
        }
        return indentation;
    }

    private boolean isMatchingFieldType(PsiField toField, PsiMethod fromGetter) {
        PsiType fromGetterReturnType = fromGetter.getReturnType();
        PsiType toFieldType = toField.getType();
        return fromGetterReturnType != null && toFieldType.isAssignableFrom(fromGetterReturnType);
    }

}
