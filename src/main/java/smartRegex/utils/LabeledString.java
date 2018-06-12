package smartRegex.utils;

public class LabeledString {

    public String string;
    public boolean accepted;

    public LabeledString(String string, boolean accepted) {
        this.string = string;
        this.accepted = accepted;
    }

    @Override
    public String toString() {
        return "smartRegex.utils.LabeledString{" +
                "string='" + string + '\'' +
                ", accepted=" + accepted +
                '}';
    }
}
