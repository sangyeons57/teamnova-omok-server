package teamnova.omok.glue.data.model;

public record UserData(String id, String name, int profileIconCode, Status status, int score){

    public static enum Status {
        PENDING,ACTIVE,INACTIVE,BLOCKED;
    }
}
