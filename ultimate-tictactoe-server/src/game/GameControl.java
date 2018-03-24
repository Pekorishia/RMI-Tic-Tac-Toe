package game;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;

import ai.AIPlayer;
import ai.AlphaBetaPruning;
import enums.GameOptions;
import enums.GameStatus;

import interfaces.IGame;
import interfaces.GameResponse;
import interfaces.IBoard;
import interfaces.IPlayer;

public class GameControl extends UnicastRemoteObject implements IGame, IBoard{

	private static final long serialVersionUID = 1L;

    private UltimateBoard tab = new UltimateBoard();   
    
	private IPlayer p1 = null;
	private IPlayer p2 = null;  

	private boolean endGame = false;
	
	private int countCredentials = 0;
	private int countPlays = 0;
	
	private GameOptions option;
	
	
	public GameControl() throws RemoteException { 
		super();
	}
	
	public String init(IPlayer player, GameOptions option) throws RemoteException {
		this.option = option;
		
		switch (option) {
			case NORMAL: 
				return initNormal(player);
			case AI:
				p2 = new AIPlayer();
				return initAI(player);
			default:
				return "ERROR";
		}
	}
		
	public GameResponse play(IPlayer player, int board, int position) throws RemoteException {
		
		switch (option) {
			case NORMAL: 
				return playNormal(player, board, position);
			case AI:
				return playAI(player, board, position);
			default:
				
				return new GameResponseImpl(null, null, "ERROR");
		}
	}

	private String initNormal(IPlayer player) throws RemoteException {
		// First player
		if(p1 == null){ 
			p1 = player;
			p1.init(this.getCredential(), X, false);
			
			while(p2 == null){
				try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
			}
		} 
		// Second player
		else if(p2 == null){
			p2 = player;
			p2.init(this.getCredential(), O, true);
			
			while(p2.isBlocked()){
				try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
			}	    
		}
		return tab.getState();
	}
	
	private String initAI(IPlayer player) throws RemoteException {
		// First player
		if(p1 == null){ 
			p1 = player;
			p1.init(this.getCredential(), X, false);
			
			// AI player
			p2 = new AIPlayer();
			p2.init(this.getCredential(), O, true);
		} 
		return tab.getState();
	}
	
//	private GameResponse getScope(String msg){
//		String text[] = new String[82];
//		text[81] = msg;
//		return new GameResponseImpl(text, null, null);
//	}
	
	private GameResponse getScopeWithtab(String msg) throws RemoteException{
		String text[] = new String[82];
		int b = 1;
		int p = 1;
		for(int i = 0; i < 81; i++, p++){
			text[i] = String.valueOf(tab.at(b).getBoardCharAt(p));
			if(p == 9){
				p = 0;
				b++;
			}
			
		}
		text[81] = msg;
		return new GameResponseImpl(text, null, null);
	}
			
	private GameResponse playNormal(IPlayer player, int board, int position) throws RemoteException {
		// If the game already ended
		if (endGame) {
			return new GameResponseImpl(null,null,"O jogo acabou.\n");
		}
		// if this is the player 1
		else if(p1.getId() == player.getId()){
						
			// If the movement is valid
			if (GameCheck.checkValidPlay(p1, board, position, tab, p1, p2)) {
				return makePlay(p1, board, position, p2);
			}
			else 
				return new GameResponseImpl(null,null,"Posicao ou jogada invalida.\n");
		}
		// if this is the player 2 
		else if(p2.getId() == player.getId()){

			if (GameCheck.checkValidPlay(p2, board, position, tab, p1, p2)) {
				return makePlay(p2, board, position, p1);
			}
			else
				return new GameResponseImpl(null,null,"Posicao ou jogada invalida.\n");
		}
		return countPlays();
	}
	
	private GameResponse playAI(IPlayer player, int board, int position) throws RemoteException {
		// If the game already ended
		if (endGame) {
			return new GameResponseImpl(null,null,"O jogo acabou.\n");
		}
		// if this is the player 1
		else if(p1.getId() == player.getId()){
						
			// If the movement is valid
			if (GameCheck.checkValidPlay(p1, board, position, tab, p1, p2)) {
				// Make the p1 movement
				makePlay(p1, board, position, p2);
				
				// make the AI movement
				player.play(board, position);
				
				// Change the turn back to the player
				p2.setBlocked(true);
				p1.setBlocked(false);	
				
				// Get the current status of the game after the play
				GameStatus status = GameCheck.checkGame(player, tab, endGame);
				String message =  generateMessage(status, player);

				//sum the moves count
				countPlays++;
				
				return getScopeWithtab(message);
			}
			else 
				return new GameResponseImpl(null,null,"Posicao ou jogada invalida.\n");
		}
		
		return countPlays();
	}

	private GameResponse makePlay(IPlayer player, int board, int position, IPlayer other) throws RemoteException {
		tab.at(board).setBoardCharAt(position, player.getName());		
		
		// Allow the player to make the movement
		player.play(board, position);
		
		// Change the turn
		player.setBlocked(true);
		other.setBlocked(false);			
		
		// Get the current status of the game after the play
		GameStatus status = GameCheck.checkGame(player, tab, endGame);
		String message =  generateMessage(status, player);				


		while(player.isBlocked()){
			try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
		}
		
		return getScopeWithtab(message);
	}
	
	private String generateMessage(GameStatus status, IPlayer player) throws RemoteException {

		switch (status) {
			case BOARD_DRAW: 
				return "\nDeu Velha!\n";
			case GAME_DRAW:
				return "\nDeu Velha!\n";
			case BOARD_WIN:
				return "\nJogador " + player.getName() + " venceu o tabuleiro " + player.getBoard() + "!\n";
			case GAME_WIN:
				return "\nJogador " + player.getName() + " venceu o jogo!\n";
			default:
		}
		return "";
	}
	
	private GameResponse countPlays() throws RemoteException {
		countPlays++;

		// If the total number of possible moves was achieved
		if (countPlays >= 82) {
			endGame = true;
			return new GameResponseImpl(null,null,"O jogo acabou.\n");
		}		
		return new GameResponseImpl(null,null,null);
	}
	
	public int getCredential() throws RemoteException {
		return ++countCredentials;
	}
}
