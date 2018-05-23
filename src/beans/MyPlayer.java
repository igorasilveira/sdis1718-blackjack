package beans;

import java.util.ArrayList;
import java.util.List;

public class MyPlayer {
	
	private int id;
	private String username;
	private List<MyCard> cards = new ArrayList<MyCard>();
	private int points;
	
	public MyPlayer(int id, String username){
		this.id = id;
		this.username = username;
	}
	
	public void giveCard(MyCard card){
		cards.add(card);
		points += card.getCost();	
	}
	
	public int getID(){
		return id;
	}
	
	public int getPoints(){
		return points;
	}
	
	public String getUsername(){
		return username;
	}
	
	public void clearHand(){
		cards.clear();
		points = 0;
	}
	public List<MyCard> getCards(){
		return cards;
	}
	
}
