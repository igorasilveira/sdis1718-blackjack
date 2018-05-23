package beans;

public class MyCard  {

    public enum Suit{
            CLUBS, DIAMONDS, HEARTS, SPADES
    }

    private String value;   // 1,2,3,4..., J,Q,K,A
    private Suit suit;    // naipe
    private int cost;

    public MyCard(int value, Suit suit){
        parseValue(value);
        this.suit = suit;
        setCost();
    }

    private void parseValue(int value) {

        if(value < 1 || value > 13){
            System.out.println("Couldn't create card as it does not contain a valid value.");
            return;
        }

        switch(value){
            case 1:
                this.value = "A";
                break;
            case 11:
                this.value = "J";
                break;
            case 12:
                this.value = "Q";
                break;
            case 13:
                this.value = "K";
                break;
            default:
                this.value = String.valueOf(value);
                break;
        }
    }

    private void setCost(){

        try {
            int valueNumber = Integer.parseInt(value);
            if(valueNumber >= 2 && valueNumber <= 10){
                cost = valueNumber;
            }
            else{
                System.out.println("Error: The card value must be between 2 ad 10");
                return;
            }
        } catch(NumberFormatException e){
            cost = 10;
        }
    }

    public String getValue(){
        return value;
    }

    public Suit getSuit(){
        return suit;
    }

    public int getCost(){
        return cost;
    }
}
