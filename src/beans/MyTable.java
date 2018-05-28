package beans;

import com.MyUtilities;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.List;

public class MyTable {

	private int id;
	private boolean isPrivate;
	private boolean hasStarted;
	private boolean canStart = false;
	private boolean dealerFinished = true;
	private int state = 0;
	private String password;
	private MyPlayer dealer = new MyPlayer();
	private List<MyPlayer> players = new ArrayList<MyPlayer>(MAX_CAPACITY);
	private List<MyPlayer> roundWinners = new ArrayList<>(MAX_CAPACITY);
	private List<MyPlayer> roundDraws = new ArrayList<>(MAX_CAPACITY);
	private List<MyPlayer> bustedPlayers = new ArrayList<>(MAX_CAPACITY);
	private MyDeck deck = new MyDeck();

	private MyPlayer currentPlayer;

	private final static int MAX_CAPACITY = 4;

	public MyTable(int id, boolean isPrivate) {
		this.id = id; this.isPrivate = isPrivate;
	}

	public void addPlayer(MyPlayer playerToAdd) {
		players.add(playerToAdd);
	}

	public void startRound() {

		if (dealerFinished) {
			state = 0;

			canStart = true;

			for (MyPlayer player:
					players) {
				if (player.getBet() < 1) {
					canStart = false;
					System.out.println("--------" + player.getUsername());
				}
			}

			if (canStart) {
				roundWinners.clear();
				bustedPlayers.clear();
				roundDraws.clear();
				dealer.getCards().clear();

				for (MyPlayer player:
						players) {
					player.getCards().clear();
				}

				deck = new MyDeck();
				deck.shuffle();
				currentPlayer = players.get(0);
				hasStarted = true;

				for (int i = 0 ; i < 2 ; i++) {
					dealer.giveCard(deck.giveCard());
					for (MyPlayer player:
							players) {
						player.giveCard(deck.giveCard());
					}
				}
			}
		}

	}

	public void nextPlayer() {

		System.out.println("NEXT PLAYER");
	    int index = players.indexOf(currentPlayer);
	    index++;

	    // its dealers turn
	    if (index > players.size() - 1) {
	    	MyPlayer dealer = new MyPlayer();
	    	dealer.setUsername(MyUtilities.sha256(id + String.valueOf(System.currentTimeMillis())));
	    	currentPlayer = dealer;
	    	playDealer();
		}
	    else {

			currentPlayer = players.get(index);

			if (currentPlayer.getCards().size() == 0 || roundWinners.contains(currentPlayer) || bustedPlayers.contains(currentPlayer))
				nextPlayer();
		}

		state++;
    }

	private void playDealer() {
		System.out.println("DEALER PLAYING");

		if (dealer.getTotal() == 21) {
			// process winners , give them back what they spent
		} else {
			dealer.giveCard(deck.giveCard());

			if (dealer.getTotal() <= 17) {
				dealer.giveCard(deck.giveCard());
			}

		}
		runFinal();

	}

	private void runFinal() {
		System.err.println("FINAL");
		hasStarted = false;
		dealerFinished = false;

		for (MyPlayer player:
			 players) {
			if (player.getTotal() <= 21) {

				if (player.getTotal() > dealer.getTotal()) {
					if (!roundWinners.contains(player))
						roundWinners.add(player);
				}
				else if (player.getTotal() == dealer.getTotal()) {
					if (!roundDraws.contains(player) && !roundWinners.contains(player))
						roundDraws.add(player);
				} else if (dealer.getTotal() > 21 && !bustedPlayers.contains(player)) {
					if (!roundWinners.contains(player))
						roundWinners.add(player);
				} else {
					if (!bustedPlayers.contains(player))
						bustedPlayers.add(player);
				}
			}
			else
				if (!bustedPlayers.contains(player))
					bustedPlayers.add(player);

		}
		resetBets();
	}

	/**
	 *
	 * @return 0 - play again, 1 - win, -1 - busted
	 */
	public void playHit() {
		currentPlayer.giveCard(deck.giveCard());

		if (currentPlayer.getTotal() == 21) {
			roundWinners.add(currentPlayer);
			nextPlayer();
		} else if (currentPlayer.getTotal() > 21) {
			nextPlayer();
		} else {

		}
		state++;
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

	public boolean isPrivate() {
		return isPrivate;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isFull() {
		return players.size() >= MAX_CAPACITY;
	}

	public boolean isInTable(String username) {
		boolean isInTable = false;

		for (MyPlayer player:
			 players) {
			if (player.getUsername().equals(username))
				isInTable = true;
		}

		return isInTable;
	}

    public MyPlayer getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(MyPlayer currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

	public boolean hasStarted() {
		return hasStarted;
	}

	public void setHasStarted(boolean hasStarted) {
		this.hasStarted = hasStarted;
	}

	public MyPlayer getDealer() {
		return dealer;
	}

	public int getState() {
		return state;
	}

	public JsonObject getJSONInfo() {

		JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
		JsonObjectBuilder dealerObject = Json.createObjectBuilder();

		// build dealer info
		JsonArrayBuilder dealerCards = Json.createArrayBuilder();

		if (hasStarted)  {
			dealerCards.add("*");

			for (int i = 1; i < dealer.getCards().size(); i++) {
				dealerCards.add(dealer.getCards().get(i).getValue()
						+ dealer.getCards().get(i).getSuit().toString());
			}
		} else {
			for (int i = 0; i < dealer.getCards().size(); i++) {
				dealerCards.add(dealer.getCards().get(i).getValue()
						+ dealer.getCards().get(i).getSuit().toString());
			}

			dealerObject.add("total", dealer.getTotal());
		}

		dealerObject.add("cards", dealerCards);

		// build players info
		JsonArrayBuilder players = Json.createArrayBuilder();
		for (MyPlayer player:
				getPlayers()) {

			// add all player cards
			JsonArrayBuilder playerCardsArray = Json.createArrayBuilder();

			for (int i = 0; i < player.getCards().size(); i++) {
				playerCardsArray.add(player.getCards().get(i).getValue()
						+ player.getCards().get(i).getSuit().toString());
			}

			JsonObjectBuilder playerObject = Json.createObjectBuilder()
					.add("username", player.getUsername())
					.add("cards", playerCardsArray.build())
					.add("total", player.getTotal())
					.add("bet", player.getBet());

			players.add(playerObject);
		}

		// add winners
		JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

		for (MyPlayer player:
			 roundWinners) {
			jsonArrayBuilder
					.add(player.getUsername());
		}

		// add busted
		JsonArrayBuilder jsonArrayBuilderBusted = Json.createArrayBuilder();

		for (MyPlayer player:
			 bustedPlayers) {
			jsonArrayBuilderBusted
					.add(player.getUsername());
		}
		// add draw
		JsonArrayBuilder jsonArrayBuilderDraw = Json.createArrayBuilder();

		for (MyPlayer player:
			 roundDraws) {
			jsonArrayBuilderDraw
					.add(player.getUsername());
		}

		// Build response object
		jsonObjectBuilder
				.add("running", hasStarted)
				.add("winners", jsonArrayBuilder.build())
				.add("busted", jsonArrayBuilderBusted.build())
				.add("draw", jsonArrayBuilderDraw.build())
				.add("dealer", dealerObject.build())
				.add("players", players.build())
				.add("state", state)
				.add("waiting", isWaiting());

		if (hasStarted)
			jsonObjectBuilder.add("playing", currentPlayer.getUsername());

		System.out.println(jsonObjectBuilder.build());

		return jsonObjectBuilder.build();
	}

	public boolean hasFinished() {
		return roundWinners.size() + bustedPlayers.size() + roundDraws.size() == players.size();
	}

	public boolean isWaiting() {
		canStart = true;

		for (MyPlayer player:
				players) {
			if (player.getBet() < 1)
				canStart = false;
		}
		return !canStart;
	}

	public void setDealerFinished(boolean dealerFinished) {
		this.dealerFinished = dealerFinished;
	}

	public void removePlayer(MyPlayer playerPut) {
		players.remove(playerPut);
		roundWinners.remove(playerPut);
		bustedPlayers.remove(playerPut);
		roundDraws.remove(playerPut);
	}

	public void resetBets() {
		for (MyPlayer player:
			 players) {
			player.setBet(0);
		}
	}
}
