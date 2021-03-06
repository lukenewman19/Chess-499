package chess;

import java.io.Serializable;
import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Represents a chess game with its significant subcomponents as instance variables.
 * 
 * @author Luke Newman 2020
 *
 */
@SuppressWarnings("unused")
public class ChessGame implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private ChessBoard chessBoard;
	private int movesMade;
	private MoveHistory moveHistory;
	private AvailableMoves availableMoves;
	// serves as the root of the move structure for the minimax algorithm
	private Move theNullMove;
	private int depth;
	public static final int MAX_DEPTH = 4;
	public static final int MIN_DEPTH = 3;
	
	transient private Logger chessLogger;
	public transient static final String LOGGER_NAME = "chessLogger";
	
	/**
	 * 
	 */
	public ChessGame() {
		chessBoard = new ChessBoard();
		chessBoard.initialize();
		moveHistory = new MoveHistory();
		availableMoves = new AvailableMoves();
		movesMade = 0;
		theNullMove = new Move(-1, -1, null, null, false, false, false);
		depth = MIN_DEPTH;
		chessLogger = Logger.getLogger(LOGGER_NAME);
	}
	
	public void setLogger() {
		chessLogger = Logger.getLogger(LOGGER_NAME);
		
	}
	/**
	 * 
	 * @return
	 */
	public int getMovesMade() {
		return movesMade;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isGameOver() {
		return availableMoves.getAvailableMoves(Color.values()[movesMade % 2]).size() == 0;
	}
	
	/**
	 * 
	 * @return
	 */
	public ChessBoard getChessBoard() {
		return chessBoard;
	}
	
	/**
	 * 
	 * @return
	 */
	public double getScore() {
		if (movesMade == 0) {
			return 0;
		}
		return BoardEvaluator.getInstance().evaluate(chessBoard, moveHistory.getLastMoveMade()) / 100.0;
	}
	
	/**
	 * 
	 * @param depth
	 */
	public void setDepth(int depth) {
		if (depth <= MAX_DEPTH && depth >= MIN_DEPTH) {
			this.depth = depth;
		}
	}
	
	/**
	 * 
	 * @param squareNumber
	 * @return
	 */
	public boolean hasAvailableMove(int squareNumber) {
		ChessPiece pieceAttemptingToMove = chessBoard.getPieceOnSquare(squareNumber);
		if (pieceAttemptingToMove == null) {
			return false;
		}
		if (availableMoves.hasAvailable(pieceAttemptingToMove)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 * @param sourceSquare
	 * @return
	 */
	public ArrayList<Integer> getAvailableTargetSquares(int sourceSquare) {
		ChessPiece pieceAttemptingToMove = chessBoard.getPieceOnSquare(sourceSquare);
		ArrayList<Integer> targetSquares = new ArrayList<Integer>();
		if (pieceAttemptingToMove == null) {
			return targetSquares;
		}
		ArrayList<Move> movesAvailable = availableMoves.getMovesForPiece(pieceAttemptingToMove);
		for(Move move: movesAvailable) {
			targetSquares.add(move.getDestination());
		}
		return targetSquares;
	}
	
	/**
	 * This method accepts a source square and a target square which represents the move a user wants to make.
	 * 	It will make the move if the move is a valid move in the context of the current game.  
	 * 
	 * @param sourceSquare
	 * @param targetSquare
	 * @return	true or false, move made or move unable to be made
	 */
	public boolean makeMove(int sourceSquare, int targetSquare) {
		
		// create Move object
		Move move = MoveFactory.getInstance().createMove(sourceSquare, targetSquare, chessBoard, moveHistory.getLastMoveMade());
		if (move == null) {
			// sourceSquare didn't even have a piece on it
			return false;
		}

		if (availableMoves.isAvailable(move)) {
			ArrayList<Move> commonMoves = null;
			ChessPiece movingPiece = move.getMovingPiece();
			if(movingPiece instanceof Pawn) {
				chessBoard.placePieceOnSquare(null, sourceSquare);
				if (move.isPawnPromotion()) {
					chessBoard.placePieceOnSquare(new Queen(movingPiece.getColor(), true), targetSquare);
					availableMoves.clearAvailableMovesForPiece(movingPiece);
				} else if(move.isEnPassant()){
					chessBoard.placePieceOnSquare(movingPiece, targetSquare);
					chessBoard.placePieceOnSquare(null, targetSquare - (8 * movingPiece.getColor().getBoardPerspective()));
				} else {
					chessBoard.placePieceOnSquare(movingPiece, targetSquare);
				}
				moveHistory.addMove(move);
				movesMade++;
			} else if (movingPiece instanceof King) {
				chessBoard.placePieceOnSquare(null, sourceSquare);
				chessBoard.placePieceOnSquare(movingPiece, targetSquare);
				// castling KingSide
				if (move.isCastling() && targetSquare > sourceSquare) {
					chessBoard.placePieceOnSquare(chessBoard.getPieceOnSquare(targetSquare + 1), sourceSquare + 1);
					chessBoard.placePieceOnSquare(null, targetSquare + 1);
				} 
				// castling QueenSide
				else if (move.isCastling()){
					chessBoard.placePieceOnSquare(chessBoard.getPieceOnSquare(targetSquare - 2), sourceSquare - 1);
					chessBoard.placePieceOnSquare(null, targetSquare - 2);
				}
				moveHistory.addMove(move);
				movesMade++;
			} else {
				chessBoard.placePieceOnSquare(movingPiece, targetSquare);
				chessBoard.placePieceOnSquare(null, sourceSquare);
				moveHistory.addMove(move);
				movesMade++;
				commonMoves = availableMoves.getCommonMovesEqualDestination(move);
			}
			if (move.getCapturedPiece() != null) {
				availableMoves.clearAvailableMovesForPiece(move.getCapturedPiece());
			}
			MoveFactory.getInstance().notateMove(move, chessBoard, commonMoves, isCheckmateOrStalemate(Color.values()[movesMade % 2]));
			//chessLogger.log(Level.INFO, availableMoves.getAvailableMoves(Color.values()[movesMade % 2]).toString());
			
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param move
	 */
	private void makeMove(Move move) {
		ArrayList<Move> commonMoves = null;
		ChessPiece movingPiece = move.getMovingPiece();
		if(movingPiece instanceof Pawn) {
			chessBoard.placePieceOnSquare(null, move.getSource());
			if (move.isPawnPromotion()) {
				chessBoard.placePieceOnSquare(new Queen(movingPiece.getColor(), true), move.getDestination());
				availableMoves.clearAvailableMovesForPiece(movingPiece);
			} else if(move.isEnPassant()){
				chessBoard.placePieceOnSquare(movingPiece, move.getDestination());
				chessBoard.placePieceOnSquare(null, move.getDestination() - (8 * movingPiece.getColor().getBoardPerspective()));
			} else {
				chessBoard.placePieceOnSquare(movingPiece, move.getDestination());
			}
			moveHistory.addMove(move);
			movesMade++;
		} else if (movingPiece instanceof King) {
			chessBoard.placePieceOnSquare(null, move.getSource());
			chessBoard.placePieceOnSquare(movingPiece, move.getDestination());
			// castling KingSide
			if (move.isCastling() && move.getDestination() > move.getSource()) {
				chessBoard.placePieceOnSquare(chessBoard.getPieceOnSquare(move.getDestination() + 1), move.getSource() + 1);
				chessBoard.placePieceOnSquare(null, move.getDestination() + 1);
			} 
			// castling QueenSide
			else if (move.isCastling()){
				chessBoard.placePieceOnSquare(chessBoard.getPieceOnSquare(move.getDestination() - 2), move.getSource() - 1);
				chessBoard.placePieceOnSquare(null, move.getDestination() - 2);
			}
			moveHistory.addMove(move);
			movesMade++;
		} else {
			chessBoard.placePieceOnSquare(movingPiece, move.getDestination());
			chessBoard.placePieceOnSquare(null, move.getSource());
			moveHistory.addMove(move);
			movesMade++;
			commonMoves = availableMoves.getCommonMovesEqualDestination(move);
		}
		if (move.getCapturedPiece() != null) {
			availableMoves.clearAvailableMovesForPiece(move.getCapturedPiece());
		}
		MoveFactory.getInstance().notateMove(move, chessBoard, commonMoves, isCheckmateOrStalemate(Color.values()[movesMade % 2]));
		move.setCounterMoves(availableMoves.getAvailableMoves(Color.values()[movesMade % 2]));
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean computerMove() {
		
		ArrayList<Move> computerMoves = availableMoves.getAvailableMoves(Color.values()[movesMade % 2]);
		
		if (movesMade == 0) {
			
			theNullMove.setCounterMoves(computerMoves);
			minimax(theNullMove, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
			
			computerMoves = theNullMove.getCounterMoves();
			Move moveToMake = computerMoves.get((int) (Math.random() * theNullMove.getCounterMoves().size()));
			for(Move move: computerMoves) {
				if(move.getScore() > moveToMake.getScore()) {
					moveToMake = move;
				}
			}
			makeMove(moveToMake.getSource(), moveToMake.getDestination());
			return true;
		} else {
			if (computerMoves.size() > 0) {
				Move moveJustMade = moveHistory.getLastMoveMade();
				
				Move root = new Move(moveJustMade.getSource(), moveJustMade.getDestination(), moveJustMade.getMovingPiece(), 
						moveJustMade.getCapturedPiece(), moveJustMade.isPawnPromotion(), moveJustMade.isEnPassant(), 
						moveJustMade.isCastling());
				root.setCounterMoves(computerMoves);
				minimax(root, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, movesMade % 2 == 0? true: false);
				Move moveToMake = computerMoves.get((int) (Math.random() * root.getCounterMoves().size()));
				for (Move move: computerMoves) {
					//System.out.println(move + " " + move.getScore());
					if (move.getScore() > moveToMake.getScore() && movesMade % 2 == 0) {
						moveToMake = move;
					}else if (move.getScore() < moveToMake.getScore() && movesMade % 2 != 0) {
						moveToMake = move;
					}
				}
				makeMove(moveToMake.getSource(), moveToMake.getDestination());
				return true;
			}
			return false;
		}
	}
	
	/**
	 * 
	 * @param move
	 * @param depth
	 * @param alpha
	 * @param beta
	 * @param maximize
	 */
	private int minimax(Move move, int depth, int alpha, int beta, boolean maximize) {
		int eval;
		if (depth == 0) {
			return BoardEvaluator.getInstance().evaluate(chessBoard, move);
		}
		
		if (move.getCounterMoves().size() == 0) {
			if(move.getNotation().contains("#")) {
				return move.getMovingPiece().getColor().getBoardPerspective() * Integer.MAX_VALUE - ((MAX_DEPTH - depth) * move.getMovingPiece().getColor().getBoardPerspective());
			} else {
				return 0;
			}
		}
		
		if (maximize) {
			
			int maxEval = Integer.MIN_VALUE;
			for (Move counterMove: move.getCounterMoves()) {
				makeMove(counterMove);
				eval = minimax(counterMove, depth - 1, alpha, beta, !maximize);
				counterMove.setScore(eval);
				maxEval = Math.max(maxEval, eval);
				alpha = Math.max(alpha, eval);
				if (beta < alpha) {
					undoMove();
					return maxEval;
				}
				undoMove();
			}
			return maxEval;
			
		} else {
			int minEval = Integer.MAX_VALUE;
			for (Move counterMove: move.getCounterMoves()) {
				makeMove(counterMove);
				eval = minimax(counterMove, depth - 1, alpha, beta, !maximize);
				counterMove.setScore(eval);
				minEval = Math.min(minEval, eval);
				beta = Math.min(beta, eval);
				if (beta < alpha) {
					undoMove();
					return minEval;
				}
				undoMove();
			}
			return minEval;
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean undoMove() {
		Move moveToUndo = moveHistory.getLastMoveMade();
		if (moveToUndo == null) {
			return false;
		}
		ChessPiece movedPiece = moveToUndo.getMovingPiece();
		ChessPiece capturedPiece = moveToUndo.getCapturedPiece();
		int previousSourceSquare = moveToUndo.getSource();
		int previousDestinationSquare = moveToUndo.getDestination();
		
		if (movedPiece instanceof Pawn) {
			chessBoard.placePieceOnSquare(null, previousDestinationSquare);
			chessBoard.placePieceOnSquare(movedPiece, previousSourceSquare);
			if(moveToUndo.isEnPassant()) {
				chessBoard.placePieceOnSquare(capturedPiece, previousDestinationSquare - (8 * movedPiece.getColor().getBoardPerspective()));
			} else if(capturedPiece != null) {
				chessBoard.placePieceOnSquare(capturedPiece, previousDestinationSquare);
			}
			moveHistory.undoMove();
			movesMade--;
		} else if(movedPiece instanceof King) {
			chessBoard.placePieceOnSquare(capturedPiece, previousDestinationSquare);
			chessBoard.placePieceOnSquare(movedPiece, previousSourceSquare);
			// undoing a KingSide castle
			if (moveToUndo.isCastling() && previousDestinationSquare > previousSourceSquare) {
				chessBoard.placePieceOnSquare(chessBoard.getPieceOnSquare(previousDestinationSquare - 1), previousDestinationSquare + 1);
				chessBoard.placePieceOnSquare(null, previousDestinationSquare - 1);
			} 
			// undoing a QueenSide castle
			else if(moveToUndo.isCastling()) { 
				chessBoard.placePieceOnSquare(chessBoard.getPieceOnSquare(previousDestinationSquare + 1), previousDestinationSquare - 2);
				chessBoard.placePieceOnSquare(null, previousDestinationSquare + 1);
			}
			moveHistory.undoMove();
			movesMade--;
		} else {
			chessBoard.placePieceOnSquare(capturedPiece, previousDestinationSquare);
			chessBoard.placePieceOnSquare(movedPiece, previousSourceSquare);
			moveHistory.undoMove();
			movesMade--;
		}
		isCheckmateOrStalemate(Color.values()[movesMade % 2]);
		return true;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean redoMove() {
		Move moveToRedo = moveHistory.getLastMoveUndone();
		if (moveToRedo == null) {
			return false;
		}
		
		ChessPiece movingPiece = moveToRedo.getMovingPiece();
		int sourceSquare = moveToRedo.getSource();
		int targetSquare = moveToRedo.getDestination();
		
		if(movingPiece instanceof Pawn) {
			chessBoard.placePieceOnSquare(null, sourceSquare);
			if (moveToRedo.isPawnPromotion()) {
				chessBoard.placePieceOnSquare(new Queen(movingPiece.getColor(), true), targetSquare);
			} else if(moveToRedo.isEnPassant()){
				chessBoard.placePieceOnSquare(movingPiece, targetSquare);
				chessBoard.placePieceOnSquare(null, targetSquare - (8 * movingPiece.getColor().getBoardPerspective()));
			} else {
				chessBoard.placePieceOnSquare(movingPiece, targetSquare);
			}
			moveHistory.redoMove();
			movesMade++;
		} else if (movingPiece instanceof King) {
			chessBoard.placePieceOnSquare(null, sourceSquare);
			chessBoard.placePieceOnSquare(movingPiece, targetSquare);
			// castling KingSide
			if (moveToRedo.isCastling() && targetSquare > sourceSquare) {
				chessBoard.placePieceOnSquare(chessBoard.getPieceOnSquare(targetSquare + 1), sourceSquare + 1);
				chessBoard.placePieceOnSquare(null, targetSquare + 1);
			} 
			// castling QueenSide
			else if (moveToRedo.isCastling()){
				chessBoard.placePieceOnSquare(chessBoard.getPieceOnSquare(targetSquare - 2), sourceSquare - 1);
				chessBoard.placePieceOnSquare(null, targetSquare - 2);
			}
			moveHistory.redoMove();
			movesMade++;
		} else {
			chessBoard.placePieceOnSquare(movingPiece, targetSquare);
			chessBoard.placePieceOnSquare(null, sourceSquare);
			moveHistory.redoMove();
			movesMade++;
		}
		if (moveToRedo.getCapturedPiece() != null) {
			availableMoves.clearAvailableMovesForPiece(moveToRedo.getCapturedPiece());
		}
		isCheckmateOrStalemate(Color.values()[movesMade % 2]);
		return true;
	}
	
	/**
	 * 
	 * @return
	 */
	public String lastMove() {
		return moveHistory.getLastMoveMade().getNotation();
	}
	
	/**
	 * 
	 * @return
	 */
	public MoveHistory getMoveHistory() {
		return moveHistory;
	}
	
	/**
	 * 
	 * @param player
	 * @return
	 */
	public boolean isKingInCheck(Color player) {
		return GameRules.getInstance().isKingInCheck(player, chessBoard);
	}
	
	/**
	 * This method must be called called prior to each move.  
	 * It updates the available moves for the player and will also detect if the game is over.
	 * 
	 * @param player
	 * @return
	 */
	public boolean isCheckmateOrStalemate(Color player) {

		ArrayList<Move> allPossibleMoves = new ArrayList<Move>();
		ArrayList<Move> possibleMovesForPiece;
		
		// For each square of chessboard
		for (int i = 0; i < 64; i++) {
			
			ChessPiece piece = chessBoard.getPieceOnSquare(i);
			if (piece != null) {
				if(piece.getColor() == player) {
					if (piece instanceof Pawn) {
						possibleMovesForPiece = GameRules.getInstance().getPossiblePawnMoves(i, chessBoard, moveHistory.getLastMoveMade());
						availableMoves.updateAvailableMoves(piece, possibleMovesForPiece);
						allPossibleMoves.addAll(possibleMovesForPiece);
					}
					else if(piece instanceof Knight) {
						possibleMovesForPiece = GameRules.getInstance().getPossibleKnightMoves(i, chessBoard);
						availableMoves.updateAvailableMoves(piece, possibleMovesForPiece);
						allPossibleMoves.addAll(possibleMovesForPiece);
					}
					else if(piece instanceof Bishop || piece instanceof Rook || piece instanceof Queen) {
						possibleMovesForPiece = GameRules.getInstance().getPossibleQRBMoves(i, chessBoard);
						availableMoves.updateAvailableMoves(piece, possibleMovesForPiece);
						allPossibleMoves.addAll(possibleMovesForPiece);
					}
					else if(piece instanceof King) {
						possibleMovesForPiece = GameRules.getInstance().getPossibleKingMoves(i, chessBoard);
						availableMoves.updateAvailableMoves(piece, possibleMovesForPiece);
						allPossibleMoves.addAll(possibleMovesForPiece);
					}
				} else {
					// it is not the other players turn so we clear all available moves for their pieces
					availableMoves.clearAvailableMovesForPiece(piece);
				}
			}
		}
		if (allPossibleMoves.isEmpty()) {
			return true;
		}
		
		return false;
	}
	
}
