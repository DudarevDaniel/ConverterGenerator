package org.dudariev.converter.generator;

import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.ui.EditorComboBoxEditor;
import com.intellij.ui.EditorComboBoxRenderer;
import com.intellij.ui.StringComboboxEditor;
import com.intellij.util.ui.JBUI;
import org.jdesktop.swingx.autocomplete.AutoCompleteComboBoxEditor;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;
import org.jetbrains.annotations.Nullable;

import javax.swing.ComboBoxEditor;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;


public class GenerateConverterDialog extends DialogWrapper {

    private JPanel dialog;
    private PsiClass psiClass;
    private ComboBox<String> convertToComboBox;
    private ComboBox<String> convertFromComboBox;

    public GenerateConverterDialog(PsiClass psiClass) {
        super(psiClass.getProject());
        this.psiClass = psiClass;
        setTitle("Select Classes for Conversion");
        dialog = new JPanel(new BorderLayout());
        dialog.setPreferredSize(JBUI.size(400, 100));
        dialog.setMinimumSize(JBUI.size(400, 100));

        PsiShortNamesCache psiShortNamesCache = PsiShortNamesCache.getInstance(psiClass.getProject());
        String[] allClassNames = psiShortNamesCache.getAllClassNames();


        convertToComboBox = new ComboBox<>(allClassNames);
        convertFromComboBox = new ComboBox<>(allClassNames);
        setupComboBox(convertToComboBox);
        setupComboBox(convertFromComboBox);


        LabeledComponent<ComboBox> convertToComponent = LabeledComponent.create(convertToComboBox, "Convert to class");
        LabeledComponent<ComboBox> convertFromComponent = LabeledComponent.create(convertFromComboBox, "Convert from class");

        dialog.add(convertToComponent, BorderLayout.NORTH);
        dialog.add(convertFromComponent, BorderLayout.SOUTH);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return dialog;
    }

    private void setupComboBox(ComboBox combobox) {
        EditorComboBoxEditor comboEditor = new StringComboboxEditor(psiClass.getProject(), StdFileTypes.JAVA, combobox);
        ObjectToStringConverter objectToStringConverter = new ObjectToStringConverter() {
            @Override
            public String getPreferredStringForItem(Object o) {
                return o.getClass().getSimpleName();
            }
        };
        ComboBoxEditor comboBoxEditor = new AutoCompleteComboBoxEditor(comboEditor, objectToStringConverter);

        combobox.setEditor(comboEditor);
        EditorComboBoxRenderer editorComboBoxRenderer = new EditorComboBoxRenderer(comboBoxEditor);
        combobox.setRenderer(editorComboBoxRenderer);

        combobox.setEditable(true);
        combobox.setMaximumRowCount(8);

        comboEditor.selectAll();
    }

    public ComboBox getConvertToComboBox() {
        return convertToComboBox;
    }

    public ComboBox getConvertFromComboBox() {
        return convertFromComboBox;
    }
}
