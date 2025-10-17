package teamnova.omok.glue.store;

public record User (String id, String name, int profileIconCode, Status status, int score){

    public static enum Status {
        PENDING,ACTIVE,INACTIVE,BLOCKED;
    }
}
