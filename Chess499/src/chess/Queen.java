package chess;
/**
 * 
 * @author Luke Newman
 *
 */
public class Queen extends ChessPiece {
	
	private boolean promoted;
	
	/**
	 * 
	 * @param color
	 */
	public Queen(Color color) {
		super(color);
		promoted = false;
	}
	
	/**
	 * 
	 * @param color
	 * @param promoted
	 */
	public Queen(Color color, boolean promoted) {
		super(color);
		this.promoted = promoted;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean wasPromoted() {
		return promoted;
	}
	
	/**
	 * 
	 */
	public String toString() {
		return super.toString() + "Queen";
	}
}
