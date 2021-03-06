//package client;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;


/**
 * A client for TCP Chess. The 
 * @author Zach, Kyle, and Steve
 *
 * Steve's attempt to add a gui
 * 2015.03.30
 *
 */
public class ChessClient {

    private static final int PORT = 9999;

    private static JFrame frame = new JFrame("TCP Chess");
    private JLabel messageLabel = new JLabel("");
    private static ImageIcon icon;
    private ImageIcon opponentIcon;

    //private Square[] board = new Square[9];
    private Square[][] board = new Square[8][8];
    private Square currentSquare;


    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    private static String username;

    /**
     * Constructs the client by connecting to a server, laying out the
     * GUI and registering GUI listeners.
     */
    public ChessClient(String serverAddress) throws Exception {

        // Setup networking
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

      
        // Layout GUI
        messageLabel.setBackground(Color.lightGray);
        frame.getContentPane().add(messageLabel, "South");

        JPanel boardPanel = new JPanel();
        boardPanel.setBackground(Color.black);
        boardPanel.setLayout(new GridLayout(board.length, board[0].length, 2, 2));
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                final int fi =i;
                final int fj =j;
                board[i][j] = new Square(i,j);
                board[i][j].addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        currentSquare = board[fi][fj];
                        out.println("Clicked " + fi+","+fj);}});
                        //out.println("MOVE " + j);}});
                boardPanel.add(board[i][j]);
            }
        }
        frame.getContentPane().add(boardPanel, "Center");
    }
    /**
     * Runs ChessClient. If args is empty, it asks the user
     * to input a server address. If args contains a value, 
     * then that value is used for the server address. 
     * It Initiates and starts the main frame. Currently a 
     * tic-tac-toe game is displayed. This needs to be changed. 
     * This also controls the continue game loop.
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) throws Exception {
        while (true) {
            // Get server address
            String serverAddress;
            if(args.length == 0) serverAddress = getServerAddress();
            else serverAddress = args[0];       
            
            // Starts the ChessClient
            ChessClient client = new ChessClient(serverAddress);
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setSize(500, 500);
            client.frame.setVisible(true);
            client.frame.setResizable(false);
            client.play();
            if (!client.wantsToPlayAgain()) {
                break;
            }
        }
    }
    /**
     * Prompts for and return the address of the server.
     */
    private static String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter Server IP Address:",
            "Welcome to TCP Chess",
            JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private static String getUsername() {
        return JOptionPane.showInputDialog(
            frame,
            "Choose a screen name:",
            "Username selection",
            JOptionPane.PLAIN_MESSAGE);
    }   
    /**
     * Accepts a player string. For example, it could accept
     * "player1 player2 player3" Adds each player into an object
     * array. Then Opens a JOptionPane with a drop down menu for
     * player1, player2, player3. Returns the string value for
     * the selected player. 
     * @param players A string value received from ChessServer
     * of all available players.
     * @return A string of the selected player name.
     */
    private static String choosePlayer(String players){
        Object[] allPlayers = players.split(" ");
        
        Object[] allPlayersMinusClient = new Object[allPlayers.length - 1];
        // Copies into the new array all values except the current username
        
        int j = 0;
        for(int i = 0; i < allPlayers.length; i++){
            if(!((String)allPlayers[i]).equals(username)){
                allPlayersMinusClient[j++] = allPlayers[i];
            }
        }
        if(allPlayersMinusClient.length != 0){
            return (String)JOptionPane.showInputDialog(
                    frame,
                    "Choose a player or select cancel to wait for another player to choose you:",
                    "Player Selection",
                    JOptionPane.PLAIN_MESSAGE,
                    icon,
                    allPlayersMinusClient,
                    allPlayersMinusClient[0]);
        }
        return "";
    }
    /**
     * A prompt with do you want to play the challenger?
     * @param challengeString The challenger's name
     * @return The selection of hte client. 0 for yes and 1 for no.
     */
    private static int challenge(String challengeString){
        String challenger = challengeString.substring(10);
        return (JOptionPane.showConfirmDialog(
                frame,
                "Would you like to play " + challenger,
                null,
                JOptionPane.YES_NO_OPTION));
    }
    
    /**
     * A prompt to choose a color for chess.
     * @return The char of the selected color. The
     * char 'b' for black and 'w' for white.
     */
    private static char chooseColor(){
        Object[] options = {"Black", "White"};
        int n = JOptionPane.showOptionDialog(frame,
                "Please Choose a Starting Color",
                "Color Selection",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,     //do not use a custom Icon
                options,  //the titles of buttons
                options[0]); //default button title
        if(n == 0) return 'b';
        return 'w';
    }
        
    /**
     * The main thread of the client will listen for messages
     * and provides a GUI for responses. This class has three
     * loop for entering user information. First, the username
     * loop continues until a correct username has been entered.
     * Second, the Playerchoice loop continues until two clients
     * have connected, and they have chosen colors. Third, the
     * game loop continues until the game has completed. 
     * 
     */
    public void play() throws Exception {
        String response;
        char color = ' ';
        messageLabel.setText("Welcome to TCP Chess");
        
        //Get Username loop
        while (true) {
            response = in.readLine();
            if (response.startsWith("GETUSERNAME")) {
                messageLabel.setText("Please enter a username");
                username = getUsername();
                out.println("USERNAME" + username);
            } else if (response.startsWith("NAMEACCEPTED")) {
                messageLabel.setText("Username Accepted");
                break;
            } else if (response.startsWith("MESSAGE")) {
                messageLabel.setText(response.substring(8));
            }
        }
        
        // Get PlayerChoice Loop
        out.println("GET_PLAYERLIST");
        while(true){
            response = in.readLine();
            if(response.startsWith("PLAYERLIST")){
                messageLabel.setText("Opponent Selection");
                String players = response.substring(10);

                // Display GUI of listed Players and respond with chosen player
                if(players.length() > 0){
                    String playerChoice = choosePlayer(players);
                    if(playerChoice != null){
                        out.println("CHOOSEN_PLAYER " + playerChoice);
                    }
                }
            }
            else if(response.startsWith("MESSAGE")){
                messageLabel.setText(response.substring(8));
            }
            else if(response.startsWith("CHALLENGE")){
                String challengerUsername = response.substring(10);
                int choice = challenge(response);
                out.println("ACCEPT_OR_REJECT_GAME " + choice + " " + challengerUsername);

            }

            // Get Color Loop
            else if(response.startsWith("CHOOSE_COLOR")){
                System.out.println("CHOOSE_COLOR");
                color = chooseColor();
                System.out.println("CHOOSE_COLOR: " + color);
                out.println("COLOR " + color);
            }
            else if(response.startsWith("SWITCH_COLOR")){
                color = response.charAt(13);
                System.out.println("SWITCHED_COLOR");
                out.println("SWITCHED_COLOR");              
            }
            else if(response.startsWith("START_GAME")){
                break;
            }
        }
        messageLabel.setText("Let's Play!");

        // Game Loop
        while (true) {
            response = in.readLine();

            if (response.startsWith("ACCEPT_MOVE")) {
                messageLabel.setText("Accepted move");
                // Move piece on client board
                char fromX = response.charAt(5);
                char fromY = response.charAt(6);
                char toX = response.charAt(7);
                char toY = response.charAt(8);
                messageLabel.setText("Accepted Move");
                move(fromX, fromY,toX, toY);

                // Repaint() the board
                
            } else if (response.startsWith("OPPONENT_MOVED")) {
                char fromX = response.charAt(15);
                char fromY = response.charAt(16);
                char toX = response.charAt(17);
                char toY = response.charAt(18);
                messageLabel.setText("Opponent Moved: " + fromX + fromY + " to " + toX + toY);

                // Move piece on client board
                move(fromX, fromY,toX, toY);
                
                // ChessBoard.repaint()

            } else if (response.startsWith("VICTORY")) {
                messageLabel.setText("You win!!!");
                break;
            } else if (response.startsWith("LOSE")) {
                messageLabel.setText("You lose!!!");
                break;
            } else if (response.startsWith("STALEMATE")) {
                messageLabel.setText("Stalemate");
                break;
            } else if (response.startsWith("MESSAGE")) {
                messageLabel.setText(response.substring(8));
            }
        }
    }
    /**
     * Not implemented yet
     * @param fromX The fromX value of the chess piece
     * @param fromY The fromY value of the chess piece
     * @param toX The toX value of the chess piece
     * @param toY The toY value of the chess piece
     */
    private void move(char fromX, char fromY, char toX, char toY) {
        // TODO Auto-generated method stub
        
    }
    /**
     * Prompts the user if they want to continue playing
     * @return 0 for yes and 1 for no.
     */
    private boolean wantsToPlayAgain() {
        int response = JOptionPane.showConfirmDialog(frame,
            "Want to play again?",
            null,
            JOptionPane.YES_NO_OPTION);
        frame.dispose();
        return response == JOptionPane.YES_OPTION;
    }

    /**
     * Graphical square in the client window.  Each square is
     * a white panel containing.  A client calls setIcon() to fill
     * it with an Icon, presumably an X or O.
     */
    static class Square extends JPanel {
        JLabel label = new JLabel((Icon)null);
        int x,y;

        public Square(int myx,int myy) {
            add(label);
            x=myx;y=myy;
            if(x%2==0){
                if(y%2==0){
                    setBackground(Color.white);

                }
                else{
                    setBackground(Color.black);
                }
            }
            else{
                if(y%2==0){
                    setBackground(Color.black);

                }
                else{
                    setBackground(Color.white);
                }
            }
        }

        /* Unnecessary I think
        public void setIcon(Icon icon) {
            label.setIcon(icon);
        }
        */
    }
}