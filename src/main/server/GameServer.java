/* Created by Michael Brozhko - finished 29/03/2021
 * 
 * Class GameServer manages incoming connections and assigns them to lobbies/sessions.
 * 
 * GameSession class represents a game session and deals with core logic of the game such as updating and using the MySQL database, dealing with messages from clients and 
 * generating random values for attack/health.
 */

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.mysql.cj.jdbc.CallableStatement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Random;

public class GameServer extends WebSocketServer {

	//private int p1rW=0;
	//private int p2rW=0;
	
	private ArrayList<GameSession> gameSessions;
	private WebSocket newClient = null;
	
	private Connection dManager = null;
	
	private class GameSession
	{
		private int sessionID = 0;

		private int p1Health = 100;
		private int p2Health = 100;
		private int playerTurn;
		private ArrayList<WebSocket> sessionConnections = new ArrayList<WebSocket>();
		
		Random r = new Random();

		public GameSession(WebSocket p1, WebSocket p2)
		{	
			sessionConnections.add(p1);
			sessionConnections.add(p2);
			
			Random rand = new Random();
			playerTurn = rand.nextInt(2);
		}
		
		
		//method onStart() is called once a game starts and performs database operations and informs players of whose turn it is
		public void onStart() {
			//send message that session started to players
			sessionConnections.get(0).send("SESSION_START");
	        sessionConnections.get(1).send("SESSION_START");
	        //identify players
	        sessionConnections.get(0).send("YOU_ARE_PLAYER 1");
	        sessionConnections.get(1).send("YOU_ARE_PLAYER 2");
	        //send message to players whose turn it is 'your turn'
	        sessionConnections.get(playerTurn).send("YOUR_TURN");
	        
	        try {
	    		PreparedStatement preparedStatement = dManager.prepareStatement("insert into  bL.sessions values (default,default,?,?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
	        
	            preparedStatement.setString(1, sessionConnections.get(0).getRemoteSocketAddress().getHostName());
	            preparedStatement.setInt(2, 100);
	            
	            if(playerTurn == 0) {
	            	preparedStatement.setString(3, "active");
	            } else {
	            	preparedStatement.setString(3, "passive");
	            }
	            preparedStatement.setString(4, sessionConnections.get(1).getRemoteSocketAddress().getHostName());
	            preparedStatement.setInt(5, 100);
	            
	            if(playerTurn == 0) {
	            	preparedStatement.setString(6, "passive");
	            } else {
	            	preparedStatement.setString(6, "active");
	            }
	            
	            preparedStatement.setString(7, "active");
	            
	            int affected = preparedStatement.executeUpdate();
	            ResultSet keys = null;
	           
	            if (affected == 1) {
	                keys = preparedStatement.getGeneratedKeys();
	                keys.next();
	                sessionID = keys.getInt(1);
	            	System.out.println("SessionID: " + sessionID);

	            } else {
	                System.err.println("No rows affected");
	            }
	    	} catch (SQLException e) {
	    		e.printStackTrace();
	    	}
		}
		
		//method performs calculation if game is done and returns true or false
		public boolean gameDone() {
			  if(p1Health <= 0) {
				  sessionConnections.get(1).send("GAME_WON");
				  sessionConnections.get(0).send("GAME_LOST");
			  } else if(p2Health <= 0) {
				  sessionConnections.get(0).send("GAME_WON");
				  sessionConnections.get(1).send("GAME_LOST");
			  } else {
				  return false;
			  }
			  sessionConnections.get(0).send("GAME_OVER");
			  sessionConnections.get(1).send("GAME_OVER");
			  return true;
		  }
		
		public void onEnd() {    
			try {				
				PreparedStatement preparedStatement = dManager.prepareStatement("UPDATE bL.sessions SET GAME_STATUS=? WHERE ID=" + sessionID);
		         
		         preparedStatement.setString(1, "passive");
		         
		         preparedStatement.executeUpdate();
		         sessionID=0;
		         System.out.println("Hello there!");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		public void onRecieve(WebSocket conn, String message) {
			 try {
					int id=0;
					String p1Name=null;
					String p2Name=null;
					String p1Status=null;
					String p2Status=null; 
					int p1h=0;
					int p2h=0;
					String gameStatus=null;

					PreparedStatement preparedStatement = dManager.prepareStatement("UPDATE bL.sessions SET PLAYER_1_HEALTH=?, PLAYER_2_HEALTH=? WHERE ID=" + sessionID);

					System.out.println("This is player turn: " + playerTurn);
					System.out.println("message received: " + message);
					int activeClient = 2;
					
					Random r = new Random();
					
					if(sessionConnections.get(0)==conn) {
						activeClient=0;
					} else if(sessionConnections.get(1)==conn) {
						activeClient=1;
					} else {
						System.out.println("Can't find client");
					}
					
					String query = "SELECT * FROM bL.sessions WHERE ID=" +sessionID;
					Statement st = dManager.createStatement();
				    ResultSet rs = st.executeQuery(query);
				    if(rs.next()) {
				    	  id=rs.getInt("ID");
				    	  p1Name=rs.getString("PLAYER_1_NAME");
				    	  p2Name=rs.getString("PLAYER_2_NAME");
				    	  
				    	  p1Status=rs.getString("PLAYER_1_STATUS");
				    	  p2Status=rs.getString("PLAYER_2_STATUS");
				    	  
				    	  p1h=rs.getInt("PLAYER_1_HEALTH");
				    	  p2h=rs.getInt("PLAYER_2_HEALTH");
				    	  
				    	  gameStatus=rs.getString("GAME_STATUS");
				    }
				    
				    	if(playerTurn == activeClient) {
				    		//checks if round done and if yes sets health back to normal and updates DB
//					    	if(roundDone()==true) {
//					    		p1H=100;
//					    		p2H=100;
//					    		preparedStatement.setInt(1, p1H);
//								preparedStatement.setInt(2, p2H);
//					    	}
				    		if(playerTurn == 0) {
				    			playerTurn = 1;
				    			if(message.equals("ATTACK")) {

				    				int hReduce = r.nextInt(60); //damage dealt for random attack
				    				int netHealth=p2h-hReduce;

				    				p1Health = p1h;
				    				p2Health = netHealth;
				    				
//				    				System.out.println("");
//				    				System.out.println("");
//				    				System.out.println("");
				    				
				    				sessionConnections.get(0).send("OTHER_HEALTH " + p2Health);
				    				sessionConnections.get(1).send("NEW_HEALTH " + p2Health);
				    		
				    				preparedStatement.setInt(1, p1Health);
				    				preparedStatement.setInt(2, p2Health);
				    			} else if(message.equals("BASE_ATTACK")) {

				    				int hReduce = 25; //damage dealt for base attack
				    				int netHealth=p2h-hReduce;

				    				p1Health = p1h;
				    				p2Health = netHealth;
				    				
				    				sessionConnections.get(0).send("OTHER_HEALTH " + p2Health);
				    				sessionConnections.get(1).send("NEW_HEALTH " + p2Health);

				    				preparedStatement.setInt(1, p1Health);
				    				preparedStatement.setInt(2, p2Health);
				    			} else if(message.equals("BOOST")) {
				    				int hBoost = r.nextInt(50); //health added for boost
				    				int netHealth=p1h+hBoost;
				    				
				    				//update for health not to go over 100 18/03/2021
				    				if(netHealth<=100) {
				    					p1Health = netHealth;
				    					p2Health = p2h;

				    					sessionConnections.get(0).send("NEW_HEALTH " + p1Health);
				    					sessionConnections.get(1).send("OTHER_HEALTH " + p1Health);

				    					preparedStatement.setInt(1, p1Health);
				    					preparedStatement.setInt(2, p2Health);
				    				} else { //else set netHealth to 100 and continue turns
				    					netHealth=100;
				    					p1Health = netHealth;
				    					p2Health = p2h;

				    					sessionConnections.get(0).send("NEW_HEALTH " + p1Health);
				    					sessionConnections.get(1).send("OTHER_HEALTH " + p1Health);

				    					preparedStatement.setInt(1, p1Health);
				    					preparedStatement.setInt(2, p2Health);
				    				}
				    				
				    			} else {
				    				System.out.println("error");
				    				preparedStatement.setString(1, "error");
				    				//s.get(0).send("HEALTH BOOST");
				    			}
				    			sessionConnections.get(1).send("YOUR_TURN");
				    			sessionConnections.get(0).send("OTHER_TURN");
				    		} else if(playerTurn==1) {
				    			playerTurn=0;
				    			if(message.equals("ATTACK")) {
				    				int hReduce = r.nextInt(60); //damage dealt for random attack
				    				int netHealth=p1h-hReduce;

				    				p1Health=netHealth;
				    				p2Health=p2h;

				    				sessionConnections.get(0).send("NEW_HEALTH "+ p1Health);
				    				sessionConnections.get(1).send("OTHER_HEALTH "+ p1Health);

				    				preparedStatement.setInt(1, p1Health);
				    				preparedStatement.setInt(2, p2Health);
				    			} else if(message.equals("BASE_ATTACK")) {
				    				int hReduce = 25; //damage dealt for base attack
				    				int netHealth=p1h-hReduce;

				    				p1Health=netHealth;
				    				p2Health=p2h;

				    				sessionConnections.get(0).send("NEW_HEALTH "+ p1Health);
				    				sessionConnections.get(1).send("OTHER_HEALTH "+ p1Health);

				    				preparedStatement.setInt(1, p1Health);
				    				preparedStatement.setInt(2, p2Health);
				    			} else if(message.equals("BOOST")) {
				    				int hBoost = r.nextInt(50); //health added for boost
				    				int netHealth=p2h+hBoost;
				    				
				    				//update for health not to go over 100 18/03/2021
				    				if(netHealth<=100) { //if health is greater than or equal to 100
				    					p1Health=p1h;
				    					p2Health=netHealth;

				    					sessionConnections.get(1).send("NEW_HEALTH " + p2Health);
				    					sessionConnections.get(0).send("OTHER_HEALTH "+ p2Health);
				    					
				    					preparedStatement.setInt(1, p1Health);
				    					preparedStatement.setInt(2, p2Health);
				    				} else { //else set netHealth to 100 and continue turns
				    					netHealth=100;
				    					p1Health=p1h;
				    					p2Health=netHealth;
			 
				    					sessionConnections.get(1).send("NEW_HEALTH " + p2Health);
				    					sessionConnections.get(0).send("OTHER_HEALTH "+ p2Health);

				    					preparedStatement.setInt(1, p1Health);
				    					preparedStatement.setInt(2, p2Health);
				    				}
				    				
				    			} else {
				    				System.out.println("error");
				    				//preparedStatement.setString(1, "error");
				    				//s.get(0).send("HEALTH REDUCE");
				    			}
				    			sessionConnections.get(0).send("YOUR_TURN");
				    			sessionConnections.get(1).send("OTHER_TURN");
				    		} else {
				    			System.out.println("error, randInt = " + playerTurn);
				    		}
				            preparedStatement.executeUpdate();
				    	} else {	    		
				    		conn.send("OUT_OF_TURN");
				    		//preparedStatement.setInt(2,);
				    	}
				    	
				    	if(gameDone()) {
				    		System.out.println("finished");
				    		sessionConnections.get(0).close();
				    		sessionConnections.get(1).close();
				    	}

			    	System.out.println("variable message= " + message);
						
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	
  public GameServer(int port) throws Exception {
    super(new InetSocketAddress(port));
    InitializeDB();
    gameSessions = new ArrayList<GameSession>();
  }

  public GameServer(InetSocketAddress address) throws Exception {
    super(address);
    InitializeDB();
  }

  public GameServer(int port, Draft_6455 draft) throws Exception {
    super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
    InitializeDB();
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    conn.send("Welcome to the server!"); //This method sends a message to the new client
    broadcast("PLAYER_JOINED " + conn.getRemoteSocketAddress().getHostName() /*+ handshake.getResourceDescriptor()*/); //This method sends a message to all clients connected
    System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
    
	if (newClient == null)
	{
		newClient = conn;
		
	} else {

		GameSession aSession = new GameSession(newClient, conn);
		newClient = null;

		aSession.onStart();
		gameSessions.add(aSession);
		System.out.println("Number of sessions: " + gameSessions.size());
    }
  }
  
  
//  public boolean roundDone() {
//	  if(p1H <= 0) {
//		  p2rW++;
//		  System.out.println("");
//		  System.out.println("Player 2 Won. Player 2 has won "+p2rW+" rounds");
//		  System.out.println("");
//		  return true;
//	  } else if(p2H <= 0) {
//		  p1rW++;
//		  System.out.println("");
//		  System.out.println("Player 1 Won. Player 1 has won "+p1rW+" rounds");
//		  System.out.println("");
//		  return true;
//	  } else {
//		  return false;
//	  }
//  }
//  
//  public boolean gameDone() {
//	  if(p2rW == 2) {
//		  s.get(1).send("GAME_WON");
//		  s.get(0).send("GAME_LOST");
//		  broadcast("GAME_OVER");
//		  System.out.println("GAME_OVER");
//
//		  return true;
//	  } else if(p1rW == 2) {
//		  s.get(0).send("GAME_WON");
//		  s.get(1).send("GAME_LOST");
//		  broadcast("GAME_OVER");
//		  System.out.println("GAME_OVER");
//
//		  return true;
//	  } else {
//		  return false;
//	  }
//  }
  
//  public boolean gameDone() {
//	  if(p1H <= 0) {
//		  s.get(1).send("GAME_WON");
//		  s.get(0).send("GAME_LOST");
//		  broadcast("GAME_OVER");
//		  System.out.println("GAME_OVER");
//		  
//		  return true;
//	  } else if(p2H <= 0) {
//		  s.get(0).send("GAME_WON");
//		  s.get(1).send("GAME_LOST");
//		  broadcast("GAME_OVER");
//		  System.out.println("GAME_OVER");
//		  
//		  return true;
//	  } else {
//		  return false;
//	  }
//  }
 
 
  private void InitializeDB() throws Exception {
	  
          // This will load the MySQL driver, each DB has its own driver
          //Class.forName("com.mysql.jdbc.Driver");
	  		Class.forName("com.mysql.cj.jdbc.Driver");
          // Setup the connection with the DB
          dManager = DriverManager.getConnection("jdbc:mysql://192.168.56.101:3306/bL"+"?user=mike&password=12345678");
          //dManager = DriverManager.getConnection("jdbc:mysql://ec2-35-178-129-3.eu-west-2.compute.amazonaws.com:3306/bL","testT", "12345678");
          //dManager = DriverManager.getConnection("jdbc:mysql://localhost:3306/bL","testT", "12345678");
  }
  
  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    broadcast(conn + " has left the room!");
    System.out.println(conn + " has left the room!");
    GameSession closingSession = findSession(conn);
    if(null != closingSession) {
    	closingSession.onEnd();
    	gameSessions.remove(closingSession);
    } else {
    	if(newClient == conn) {
    		newClient=null;
    	} else {
    		System.out.println("Couldnt find active connection: " + conn);
    	}
    }
  }
  
  public String CalculateHealth(PreparedStatement ps){
	  ps.toString();
	  return null;
  }
  
  
  
  
  // find a specified connection in a game session and return it
  public GameSession findSession(WebSocket conn) {
	  //look for the connection passed as a parameter in each game session
	  for(GameSession gameSession: gameSessions) {
		  if(gameSession.sessionConnections.get(0).equals(conn)) { //if the passed connection equals the connection in a specific session
			  return gameSession;
		  } else if(gameSession.sessionConnections.get(1).equals(conn)){
			  return gameSession;
		  }
	  }
	  return null;
  }
    
  @Override
  public void onMessage(WebSocket conn, String message) {
	  
	findSession(conn).onRecieve(conn, message);
	//broadcast(message);
    System.out.println(conn.getRemoteSocketAddress().getHostName() + ": " + message);
  }

  @Override
  public void onMessage(WebSocket conn, ByteBuffer message) {
    broadcast(message.array());
    System.out.println(conn + ": " + message);
  }


  public static void main(String[] args) throws Exception {
    int port = 8887; // 843 flash policy port
    try {
      port = Integer.parseInt(args[0]);
    } catch (Exception ex) {
    }
    GameServer s = new GameServer(port);
    s.start();
    System.out.println("GameServer started on port: " + s.getPort());

    BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      String in = sysin.readLine();
      s.broadcast(in);
      if (in.equals("exit")) {
        s.stop(1000);
        break;
      }
    }
  }
  
  
  @Override
  public void onError(WebSocket conn, Exception ex) {
    ex.printStackTrace();
    if (conn != null) {
      // some errors like port binding failed may not be assignable to a specific websocket
    }
  }

  @Override
  public void onStart() {
    System.out.println("Server started!");
    setConnectionLostTimeout(0);
    setConnectionLostTimeout(100);
  }
  

}
