package beans;

public class TestBeans {

	public static void main(String[] args) {

		MyDeck deck = new MyDeck();
		int i;
		System.out.println("Cards:" + deck.numberOfCards());
		for (i = 0; i < deck.numberOfCards(); i++) {
			System.out.println("Card n:" + deck.getDeck().get(i).getValue() + " - suit: "+ deck.getDeck().get(i).getSuit());
		}

	}

}
