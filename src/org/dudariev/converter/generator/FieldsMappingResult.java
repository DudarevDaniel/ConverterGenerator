package org.dudariev.converter.generator;

import com.intellij.psi.PsiMethod;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FieldsMappingResult {

    /**
     * Key is a Setter method for To class field
     * Value is a Getter method for From class field
     */
    private Map<PsiMethod, PsiMethod> mappedFields = new HashMap<>();

    private List<String> notMappedToFields = new ArrayList<>();
    private List<String> notMappedFromFields = new ArrayList<>();

    public Map<PsiMethod, PsiMethod> getMappedFields() {
        return mappedFields;
    }

    public List<String> getNotMappedToFields() {
        return notMappedToFields;
    }

    public List<String> getNotMappedFromFields() {
        return notMappedFromFields;
    }

    public void addMappedField(PsiMethod toSetter, PsiMethod fromGetter) {
        mappedFields.put(toSetter, fromGetter);
    }

    public void addNotMappedToField(String toField) {
        notMappedToFields.add(toField);
    }

    public void addNotMappedFromField(String fromField) {
        notMappedFromFields.add(fromField);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        FieldsMappingResult that = (FieldsMappingResult) o;

        return new EqualsBuilder()
                .append(mappedFields, that.mappedFields)
                .append(notMappedToFields, that.notMappedToFields)
                .append(notMappedFromFields, that.notMappedFromFields)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(mappedFields)
                .append(notMappedToFields)
                .append(notMappedFromFields)
                .toHashCode();
    }
}
