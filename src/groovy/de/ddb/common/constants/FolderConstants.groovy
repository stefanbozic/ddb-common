package de.ddb.common.constants

enum FolderConstants {

    MAIN_BOOKMARKS_FOLDER("favorites"),
    PUBLISHING_NAME_USERNAME("username"),
    PUBLISHING_NAME_FULLNAME("fullname")

    private value

    public FolderConstants(String value) {
        this.value = value
    }

    public String getValue() {
        return this.value
    }

    public static boolean isValidPublishingName(String check){
        if(check == PUBLISHING_NAME_USERNAME.value
        || check == PUBLISHING_NAME_FULLNAME.value) {
            return true
        }
        return false
    }
}
