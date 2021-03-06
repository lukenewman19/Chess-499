package controller;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import chess.ChessGame;
import chess.ChessPiece;
import chess.Color;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import view.ChessAppMenuBar;
import view.ChessBoardUI;
import view.LoggerPane;
import view.MoveHistoryTable;
import view.UtilityPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.activation.DataHandler;

/**
 * 
 * @author Luke Newman
 *
 */
public class GameController {


	private static GameController gameController;
	private ChessGame game;
	private ChessBoardUI board;
	private ChessAppMenuBar menu;
	private MoveHistoryTable moveHistoryTable;
	private UtilityPane utilityPane;
	private LoggerPane loggerPane;
	private Stage stage;
	private Logger chessLogger;
	private ChessBoardController chessBoardController;
	private boolean busy;
	private int mode;
	private static final int HUMAN_MODE = 0;
	private static final int COMPUTER_MODE_BLACK = 1;
	private static final int COMPUTER_MODE_WHITE = 2;
	
	/**
	 * 
	 * @param stage
	 */
	private GameController() {
		
		chessLogger = Logger.getLogger(ChessGame.LOGGER_NAME);
		
		// the model
		game = new ChessGame();
		game.isCheckmateOrStalemate(Color.WHITE);
		
		chessBoardController = new ChessBoardController();
		// the view
		board = new ChessBoardUI(chessBoardController);
		menu = new ChessAppMenuBar(new MenuBarController());
		moveHistoryTable = new MoveHistoryTable();
		utilityPane = new UtilityPane(new UtilityPaneController());
		utilityPane.setMargins();
		loggerPane = new LoggerPane();
		
		ChessPieceImages.setImages();
		busy = false;
		mode = HUMAN_MODE;
		updateBoard();
	}
	
	public static GameController getInstance() {
		if (gameController == null) {
			gameController = new GameController();
		}
		return gameController;
	}
	
	public void setStage(Stage stage) {
		this.stage = stage;
	}
	
	public void setLogger() {
		try {
			chessLogger.addHandler(ChessLogHandler.getInstance());
		} catch (Exception ex) {
			
		}
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
	 * @return
	 */
	public LoggerPane getLoggerPane() {
		return loggerPane;
	}
	
	/**
	 * 
	 */
	private void updateBoard() {
		ChessPiece piece;
		for (int i = 0; i < 64; i++) {
			if ((piece = game.getChessBoard().getPieceOnSquare(i)) != null) {
				ImageView pieceImage = ChessPieceImages.getImageView(piece.toString());
				board.add(pieceImage, i);
			} else {
				board.clearImageFromSquare(i);
			}
		}
		utilityPane.setScore(game.getScore());
	}
	
	/**
	 * 
	 */
	public void depthChanged(int newDepth) {
		if (busy) {
			return;
		}
		game.setDepth(newDepth);
	}
	
	/**
	 * 
	 * @author Luke Newman
	 *
	 */
	private class ChessBoardController implements EventHandler<MouseEvent>{
		
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
		
		/**
		 * 
		 */
		public void clearSelection() {
			
			if (sourceSquare != -1) {
				sourceSquare = -1;
				board.flipAvailabilityIndicator(targetSquares);
				targetSquares.clear();
			}
			
		}
		
		@Override
		public void handle(MouseEvent event) {
			
			if (busy) {
				return;
			}
			
			StackPane square = (StackPane) event.getSource();
			int squareID = board.getSquareID(square);
			if (sourceSquare == -1) {
				if(game.hasAvailableMove(squareID)) {
					targetSquares = game.getAvailableTargetSquares(squareID);
					if (!targetSquares.isEmpty()) {
						sourceSquare = squareID;
						board.flipAvailabilityIndicator(targetSquares);
					}
				}
				
			}
			else {
				targetSquare = squareID;
				board.flipAvailabilityIndicator(targetSquares);
				if (targetSquares.contains( (Integer) targetSquare)) {
					if(game.makeMove(sourceSquare, targetSquare)) {
						sourceSquare = -1;
						targetSquare = -1;
						updateBoard();
						moveHistoryTable.addMove(game.lastMove());
						if (game.isGameOver()) {
							if (game.lastMove().contains("#")) {
								chessLogger.log(Level.INFO, "PLAYER " + Color.values()[(1 + game.getMovesMade()) % 2] + " WINS!");
							} else {
							chessLogger.log(Level.INFO, "STALEMATE!");
							}
						}
						// thanks to https://community.oracle.com/thread/2300778
						
						if (mode == COMPUTER_MODE_BLACK || mode == COMPUTER_MODE_WHITE) {
							
							busy = true;
							new Thread(new Runnable() {
								
								public void run() {
									
									if (game.computerMove()) {
										Platform.runLater(new Runnable() {
											public void run() {
												moveHistoryTable.addMove(game.lastMove());
												updateBoard();
												if (game.isGameOver()) {
													if (game.lastMove().contains("#")) {
														chessLogger.log(Level.INFO, "PLAYER " + Color.values()[(1 + game.getMovesMade()) % 2] + " WINS!");
													} else {
													chessLogger.log(Level.INFO, "STALEMATE!");
													}
												}
											}
										});
									}
											
									busy = false;		
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
	private class MenuBarController implements EventHandler<ActionEvent>{

		@Override
		public void handle(ActionEvent event) {
			if (busy) {
				return;
			}
			MenuItem sourceObject = (MenuItem) event.getSource();
			switch (sourceObject.getText()) {
			case "New Game":
				game = new ChessGame();
				chessLogger.log(Level.INFO, "STARTING NEW GAME!");
				game.isCheckmateOrStalemate(Color.WHITE);
				updateBoard();
				chessBoardController.clearSelection();
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
				if (Persistence.getInstance().saveGame(fileToSave, game)) {
					chessLogger.log(Level.INFO, "SAVED GAME SUCCESSFULLY!");
				}
				break;
			case "Load Game":
				FileChooser fileChooserLoad = new FileChooser();
				fileChooserLoad.setInitialDirectory(new File("./ChessGames"));
				File fileToLoad = fileChooserLoad.showOpenDialog(stage);
				ChessGame temp = Persistence.getInstance().loadGame(fileToLoad);
				if (temp != null) {
					game = temp;
					game.setLogger();
					moveHistoryTable.loadMoves(game.getMoveHistory().getMovesMade());
					updateBoard();
					chessBoardController.clearSelection();
					mode = HUMAN_MODE;
					chessLogger.log(Level.INFO, "GAME LOADED SUCCESSFULLY!");
				}
				break;
			case "Play as Black":
				mode = COMPUTER_MODE_BLACK;
				chessBoardController.clearSelection();
				if (!board.getFlipped()) {
					board.flipBoard();
					updateBoard();
				}
				if (game.getMovesMade() % 2 == 0) {
					
					busy = true;
					new Thread(new Runnable() {
						
						public void run() {
							
							if (game.computerMove()) {
								Platform.runLater(new Runnable() {
									public void run() {
										moveHistoryTable.addMove(game.lastMove());
										updateBoard();
										if (game.isGameOver()) {
											if (game.lastMove().contains("#")) {
												chessLogger.log(Level.INFO, "PLAYER " + Color.values()[(1 + game.getMovesMade()) % 2] + " WINS!");
											} else {
											chessLogger.log(Level.INFO, "STALEMATE!");
											}
										}
									}
								});
							}
									
							busy = false;		
						}
								
					}).start();
						//moveHistoryTable.addMove(game.lastMove());
						//updateBoard();
					
				}
				break;
			case "Play as White":
				mode = COMPUTER_MODE_WHITE;
				chessBoardController.clearSelection();
				if (board.getFlipped()) {
					board.flipBoard();
					updateBoard();
				}
				if (game.getMovesMade() % 2 == 1) {
					
					busy = true;
					new Thread(new Runnable() {
						
						public void run() {
							
							if (game.computerMove()) {
								Platform.runLater(new Runnable() {
									public void run() {
										moveHistoryTable.addMove(game.lastMove());
										updateBoard();
										if (game.isGameOver()) {
											if (game.lastMove().contains("#")) {
												chessLogger.log(Level.INFO, "PLAYER " + Color.values()[(1 + game.getMovesMade()) % 2] + " WINS!");
											} else {
											chessLogger.log(Level.INFO, "STALEMATE!");
											}
										}
									}
								});
							}
									
							busy = false;		
						}
								
					}).start();
					
				}
				break;
			case "Human Mode":
				mode = HUMAN_MODE;
				break;
			case "Give Feedback":
				Stage emailStage = new Stage();
				
				VBox box = new VBox();
				HBox email = new HBox();
				HBox subject = new HBox();
				HBox body = new HBox();
				Label emailLabel = new Label("Your Email: ");
				emailLabel.setMinWidth(90);
				Label subjectLabel = new Label("Subject: ");
				subjectLabel.setMinWidth(90);
				Label bodyLabel = new Label("Message: ");
				bodyLabel.setMinWidth(90);
				TextField emailText = new TextField("Enter your email.");
				TextField emailSubject = new TextField();
				TextArea emailBodyMessage = new TextArea();
				email.getChildren().add(emailLabel);
				email.getChildren().add(emailText);
				subject.getChildren().add(subjectLabel);
				subject.getChildren().add(emailSubject);
				body.getChildren().add(bodyLabel);
				body.getChildren().add(emailBodyMessage);
				Button send = new Button("Send");
				box.getChildren().add(email);
				box.getChildren().add(subject);
				box.getChildren().add(body);
				box.getChildren().add(send);
				
				send.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {
						SendEmail sm = new SendEmail(emailSubject.getText(), emailText.getText() + "\n" + emailBodyMessage.getText());
						sm.send();
						emailStage.close();
					}
					
				});
				
				Scene emailScene = new Scene(box, 600, 300);
				emailStage.setScene(emailScene);
				emailStage.show();
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
	private class UtilityPaneController implements EventHandler<ActionEvent>{

		@Override
		public void handle(ActionEvent event) {
			if (busy) {
				return;
			}
			switch( ((Labeled) event.getSource()).getText()) {
			case "Undo":
				chessBoardController.clearSelection();
				if(mode == COMPUTER_MODE_BLACK && game.getMovesMade() < 2) {
					break;
				}
				if (game.lastMove().contains("#")) {
					if(mode == COMPUTER_MODE_BLACK && game.getMovesMade() % 2 == 0) {
						game.undoMove();
						moveHistoryTable.undoMove();
					}
					else if (mode == COMPUTER_MODE_WHITE && game.getMovesMade() % 2 == 1) {
						game.undoMove();
						moveHistoryTable.undoMove();
					}
					else {
						game.undoMove();
						moveHistoryTable.undoMove();
						if (mode == COMPUTER_MODE_BLACK || mode == COMPUTER_MODE_WHITE) {
							game.undoMove();
							moveHistoryTable.undoMove();
						}
					}
				}
				else if (game.undoMove()) {
					moveHistoryTable.undoMove();
					if (mode == COMPUTER_MODE_BLACK || mode == COMPUTER_MODE_WHITE) {
						if (game.undoMove()) {
							moveHistoryTable.undoMove();
						}
					}
				}
				updateBoard();
				break;
			case "Redo":
				chessBoardController.clearSelection();
				if (mode == COMPUTER_MODE_BLACK || mode == COMPUTER_MODE_WHITE) {
					if (game.redoMove()) {
						moveHistoryTable.addMove(game.lastMove());
						if(game.redoMove()) {
							moveHistoryTable.addMove(game.lastMove());
						}
						updateBoard();
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
