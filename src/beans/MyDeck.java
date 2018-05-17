package beans;

import java.util.ArrayList;
import java.util.List;
import beans.MyCard.Suit;
import java.util.Collections;

public class MyDeck {

    private final static int NO_SUITS = 4;
    private final static int NO_CARDS_PER_SUIT = 13;

	private List<MyCard> deck = new ArrayList<MyCard>();

	public MyDeck() {
        Suit suit;

        for(int i = 0; i < NO_SUITS; i++){
            suit = Suit.values()[i];
            createSuitCards(suit);
        }
	}

    private void createSuitCards(Suit suit){

        for(int i = 1; i <= NO_CARDS_PER_SUIT; i++){
            deck.add(new MyCard(i, suit));
        }
    }

	public List<MyCard> getDeck() {
		return deck;
	}

	public void setDeck(List<MyCard> deck) {
		this.deck = deck;
	}

	public int numberOfCards() {
		return this.deck.size();
	}

    public void shuffle(){
        Collections.shuffle(deck);
    }
}
