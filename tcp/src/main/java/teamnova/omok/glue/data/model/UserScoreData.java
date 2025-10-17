package teamnova.omok.glue.data.model;

public record UserScoreData(int score) {
    public static UserScoreData of(int score){
        return new UserScoreData(score);
    }
    public static UserScoreData getDefault(){
        return new UserScoreData(0);
    }
}
