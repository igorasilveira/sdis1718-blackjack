package beans;

public class MyCard  {

    public enum Suit{
            CLUBS, DIAMONDS, HEARTS, SPADES
    }

    private final String value;   // 1,2,3,4..., J,Q,K,A
    private final Suit suit;    // naipe
    private int cost;

    public MyCard(String value, Suit suit){
        this.value = value;
        this.suit = suit;
        setCost();
    }

    private void setCost(){

        try {
            int valueNumber = Integer.parseInt(value);
            if(valueNumber >= 2 && valueNumber <= 10){
                cost = valueNumber;
            }
            else{
                System.out.println("Error: The card value must be between 2 ad 10");
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
