package beans;

import java.util.ArrayList;
import java.util.List;

public class MyTable {

	private int id;
	private List<MyPlayer> players = new ArrayList<MyPlayer>(4);
	private MyDeck deck = new MyDeck();

	private final static int MAX_CAPACITY = 4;

	public MyTable(int id) {
		this.id = id;
	}

	public boolean addPlayer(MyPlayer playerToAdd) {

		if (players.size() == MAX_CAPACITY)
			return false;

		players.add(playerToAdd);
		return true;
	}

	public void startRound() {
		this.deck.shuffle();
		// .....
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public MyDeck getDeck() {
		return deck;
	}

	public void setDeck(MyDeck deck) {
		this.deck = deck;
	}

	public void setPlayers(List<MyPlayer> players) {
		this.players = players;
	}

	public List<MyPlayer> getPlayers() {
		return players;
	}
}
