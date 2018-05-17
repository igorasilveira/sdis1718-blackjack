package beans;

import java.util.ArrayList;

public class MyDeck {

	private ArrayList<MyCard> deck = new ArrayList<MyCard>();

	public MyDeck() {
		String 
		MyCard card = new MyCard();
		for (int i = 1; i <= 13; i++) {
			card.deck.add(card);
		}
	}

	public ArrayList<MyCard> getDeck() {
		return deck;
	}

	public void setDeck(ArrayList<MyCard> deck) {
		this.deck = deck;
	}

	public int numberOfCards() {
		return this.deck.size();
	}

}
