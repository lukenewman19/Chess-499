package chess;

public class ChessBoard {
	
	private Square[][] chessBoard;
	private int whiteKingPosition;
	private int blackKingPosition;
	
	public ChessBoard() {
		chessBoard = new Square[8][8];
		for (int i = 0; i < 64; i++) {
			chessBoard[i/8][i%8]= new Square(i);
		}
		
	}
	
	public ChessPiece getPieceOnSquare(int squareNumber) {
		return chessBoard[squareNumber/8][squareNumber%8].getPiece();
	}
	
	public void placePieceOnSquare(ChessPiece piece, int squareNumber) {
		
		// row = squareNumber/8 and column = squareNumber%8
		chessBoard[squareNumber/8][squareNumber%8].setPiece(piece);
		if (piece instanceof King) {
			if (piece.getColor() == Color.BLACK) {
				setBlackKingPosition(squareNumber);
			} else {
				setWhiteKingPosition(squareNumber);
			}
		}
		
	}
	
	public int getWhiteKingPosition() {
		return whiteKingPosition;
	}

	private void setWhiteKingPosition(int whiteKingPosition) {
		this.whiteKingPosition = whiteKingPosition;
	}

	public int getBlackKingPosition() {
		return blackKingPosition;
	}

	private void setBlackKingPosition(int blackKingPosition) {
		this.blackKingPosition = blackKingPosition;
	}
	
	public String toString() {
		int rowCounter = 7;
		String output = "";
		while(rowCounter > -1) {
			for(Square square: chessBoard[rowCounter]) {
				if(square.getPiece() != null) {
					output += String.format("%1$14s", square.getPiece().toString()); 
				}
				else {
					output += String.format("%1$14s", square.getSquareNumber());
				}
			}
			rowCounter--;
			output += "\n";
		}
		return output;
	}
}