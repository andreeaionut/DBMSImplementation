public enum FieldType {INT("I", "int"), CHAR("C", "char"), DATE("D", "date");

    private java.lang.String code;
    private java.lang.String text;

    private FieldType(java.lang.String code, java.lang.String text) {
        this.code = code;
        this.text = text;
    }

    public java.lang.String getCode() {
        return code;
    }

    public java.lang.String getText() {
        return text;
    }

    public static FieldType getByCode(java.lang.String fieldCode) {
        for (FieldType f : FieldType.values()) {
            if (f.code.equals(fieldCode)) {
                return f;
            }
        }
        return null;
    }

    public boolean contains(String fieldType){
        for(FieldType f : FieldType.values()){
            if(f.text.equals(fieldType)){
                return true;
            }
        }
        return false;
    }

    @Override
    public java.lang.String toString() {
        return this.text;
    }
}
