package beans;

import java.util.ArrayList;
import java.util.List;

public class MyPlayer {
	
	private String username;
	private String password;
	private List<MyCard> cards = new ArrayList<MyCard>();
	private int points;
	
	public MyPlayer(String username, String password){
		this.username = username;
		this.password = password;
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

	public void clearHand(){
		cards.clear();
		points = 0;
	}
	public List<MyCard> getCards(){
		return cards;
	}
}
