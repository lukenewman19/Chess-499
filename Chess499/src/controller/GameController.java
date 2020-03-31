package controller;

import java.io.File;
import java.util.ArrayList;

import chess.ChessGame;
import chess.ChessPiece;
import chess.Color;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import view.ChessAppMenuBar;
import view.ChessBoardUI;
import view.MoveHistoryTable;
import view.UtilityPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * 
 * @author Luke Newman
 *
 */
public class GameController {


	private ChessGame game;
	private ChessBoardUI board;
	private ChessAppMenuBar menu;
	private MoveHistoryTable moveHistoryTable;
	private UtilityPane utilityPane;
	private Stage stage;
	
	private int mode;
	private static final int HUMAN_MODE = 0;
	private static final int COMPUTER_MODE_BLACK = 1;
	private static final int COMPUTER_MODE_WHITE = 2;
	
	/**
	 * 
	 * @param stage
	 */
	public GameController(Stage stage) {
		// the model
		game = new ChessGame();
		game.isCheckmateOrStalemate(Color.WHITE);
		
		this.stage = stage;
		// the view
		board = new ChessBoardUI(new ChessBoardController());
		menu = new ChessAppMenuBar(new MenuBarController());
		moveHistoryTable = new MoveHistoryTable();
		utilityPane = new UtilityPane(new UtilityPaneController());
		
		ChessPieceImages.setImages();
		mode = HUMAN_MODE;
		updateBoard();
	}
	
	/**
	 * 
	 * @return
	 */
	public ChessBoardUI getChessBoardView() {
		return board;
	}
	
	/**
	 * 
	 * @return
	 */
	public ChessAppMenuBar getMenuBar() {
		return menu;
	}
	
	/**
	 * 
	 * @return
	 */
	public MoveHistoryTable getMoveHistoryTable() {
		return moveHistoryTable;
	}
	
	/**
	 * 
	 * @return
	 */
	public UtilityPane getUtilityPane() {
		return utilityPane;
	}
	
	/**
	 * 
	 */
	public void updateBoard() {
		ChessPiece piece;
		for (int i = 0; i < 64; i++) {
			if ((piece = game.getChessBoard().getPieceOnSquare(i)) != null) {
				ImageView pieceImage = ChessPieceImages.getImageView(piece.toString());
				board.add(pieceImage, i);
			} else {
				board.clearImageFromSquare(i);
			}
		}
	}
	
	/**
	 * 
	 * @author Luke Newman
	 *
	 */
	public class ChessBoardController implements EventHandler<MouseEvent>{
		
		private int sourceSquare;
		private int targetSquare;
		ArrayList<Integer> targetSquares = new ArrayList<Integer>();
		
		/**
		 * 
		 */
		public ChessBoardController() {
			sourceSquare = -1;
			targetSquare = -1;
		}
		
		@Override
		public void handle(MouseEvent event) {
			StackPane square = (StackPane) event.getSource();
			int squareID = board.getSquareID(square);
			if (sourceSquare == -1) {
				if(game.hasAvailableMove(squareID)) {
					targetSquares = game.getAvailableTargetSquares(squareID);
					if (!targetSquares.isEmpty()) {
						sourceSquare = squareID;
					}
				}
				
			}
			else {
				targetSquare = squareID;
				if (targetSquares.contains( (Integer) targetSquare)) {
					if(game.makeMove(sourceSquare, targetSquare)) {
						sourceSquare = -1;
						targetSquare = -1;
						updateBoard();
						moveHistoryTable.addMove(game.lastMove());
						// thanks to https://community.oracle.com/thread/2300778
						if (mode == COMPUTER_MODE_BLACK || mode == COMPUTER_MODE_WHITE) {
							new Thread(new Runnable() {
								
								public void run() {
									
									game.computerMove();
									
									Platform.runLater(new Runnable() {
										public void run() {
											moveHistoryTable.addMove(game.lastMove());
											updateBoard();
										}
									});
								}
								
							}).start();
						}
					}
					
					
				}else {
					sourceSquare = -1;
					targetSquare = -1;
				}
				
			}
			
			
		}
		
	}
	
	/**
	 * 
	 * @author Luke Newman
	 *
	 */
	public class MenuBarController implements EventHandler<ActionEvent>{

		@Override
		public void handle(ActionEvent event) {
			
			MenuItem sourceObject = (MenuItem) event.getSource();
			switch (sourceObject.getText()) {
			case "New Game":
				game = new ChessGame();
				game.isCheckmateOrStalemate(Color.WHITE);
				updateBoard();
				moveHistoryTable.clear();
				mode = HUMAN_MODE;
				break;
			case "Flip Board":
				board.flipBoard();
				updateBoard();
				break;
			case "Save Game":
				FileChooser fileChooser = new FileChooser();
				fileChooser.setInitialDirectory(new File("./ChessGames"));
				File fileToSave = fileChooser.showSaveDialog(stage);
				Persistence.getInstance().saveGame(fileToSave, game);
				break;
			case "Load Game":
				FileChooser fileChooserLoad = new FileChooser();
				fileChooserLoad.setInitialDirectory(new File("./ChessGames"));
				File fileToLoad = fileChooserLoad.showOpenDialog(stage);
				game = Persistence.getInstance().loadGame(fileToLoad);
				moveHistoryTable.loadMoves(game.getMoveHistory().getMovesMade());
				updateBoard();
				mode = HUMAN_MODE;
				break;
			case "Play as Black":
				mode = COMPUTER_MODE_BLACK;
				if (game.getMovesMade() % 2 == 0) {
					if(game.computerMove()) {
						moveHistoryTable.addMove(game.lastMove());
						updateBoard();
					}
				}
				break;
			case "Play as White":
				mode = COMPUTER_MODE_WHITE;
				if (game.getMovesMade() % 2 == 1) {
					if(game.computerMove()) {
						moveHistoryTable.addMove(game.lastMove());
						updateBoard();
					}
				}
				break;
			case "Human Mode":
				mode = HUMAN_MODE;
				break;
			default:
				break;	
			}
			
		}
		
	}
	
	/**
	 * 
	 * @author Luke Newman
	 *
	 */
	public class UtilityPaneController implements EventHandler<ActionEvent>{

		@Override
		public void handle(ActionEvent event) {
			
			switch( ((Labeled) event.getSource()).getText()) {
			case "Undo":
				if(mode == COMPUTER_MODE_BLACK && game.getMovesMade() < 2) {
					break;
				}
				if (game.undoMove()) {
					moveHistoryTable.undoMove();
					if (mode == COMPUTER_MODE_BLACK || mode == COMPUTER_MODE_WHITE) {
						if (game.undoMove()) {
							moveHistoryTable.undoMove();
						}
					}
					updateBoard();
				}
				break;
			case "Redo":
				if (mode == COMPUTER_MODE_BLACK || mode == COMPUTER_MODE_WHITE) {
					if (game.redoMove()) {
						moveHistoryTable.addMove(game.lastMove());
						updateBoard();
						if(game.redoMove()) {
							moveHistoryTable.addMove(game.lastMove());
							updateBoard();
						}
					}
				}else {
					if(game.redoMove()) {
						moveHistoryTable.addMove(game.lastMove());
						updateBoard();
					}
				}
				
				break;
			}
		}
		
	}
	
}
