package beans;

import java.util.ArrayList;
import java.util.List;

public class MyPlayer {
	
	private String username;
	private String password;
	private String token;
	private List<MyCard> cards = new ArrayList<MyCard>();
	private int bet;
	private int points;
	private int credits;

	public MyPlayer(){
	}


	public void giveCard(MyCard card){
		cards.add(card);
		points += card.getCost();	
	}
	
	public int getPoints(){
		return points;
	}
	
	public String getUsername(){
		return username;
	}

	public String getPassword() {
		return password;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getCredits() {
		return credits;
	}

	public void setCredits(int credits) {
		this.credits = credits;
	}

	public void clearHand(){
		cards.clear();
		points = 0;
	}
	public List<MyCard> getCards(){
		return cards;
	}

	public int getTotal() {
		int total = 0;
		for (MyCard card:
			 cards) {
			if (card.isAce()) {
				if ((total + 10) < 21)
					total = total + 10;
				else
					total = total + 1;
			} else
				total = total + card.getCost();
		}

		return total;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public int getBet() {
		return bet;
	}

	public void setBet(int bet) {
		this.bet = bet;
	}
}
