package multicast_chat.network;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import multicast_chat.interfaces.UserInterface;
import multicast_chat.network.MulticastConnection.Message;

public class MessagesManager {
	
	
	public static final long TIME_BETWEEN_MESSAGE_FOR_SPAM = 10000; // 5 secondes
	
	public static final long HISTORY_LENGTH = 300;
	
	private MulticastConnection connection;
	
	private List<Message> messagesHistory = new ArrayList<Message>();
	private List<ReadOnlyMessage> readOnlyMessageHistory = new ArrayList<ReadOnlyMessage>();
	
	private String name = null;
	private Map<String, String> aliases = new HashMap<String, String>();
	
	private List<UserInterface> interfaces = new ArrayList<UserInterface>();
	
	public final Object messagesHistoryLocker = new Object();
	
	
	
	public MessagesManager(MulticastConnection co) {
		
		connection = co;
		
	}
	
	
	
	public MulticastConnection getConnection() { return connection; }
	
	
	
	/**
	 * Envoi le message passé en paramètre. Si un nom d'affichage a été défini avec {@link #setDisplayName(String)},
	 * celui-ci sera ajouté en début de chaine, avant le message (avec <code>" : "</code> comme séparateur).
	 * @param message
	 */
	public void send(String message) {
		try {
			connection.send(((name != null)?(name+" : "):"")+message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	public void setDisplayName(String n, UserInterface ui) {
		name = n;
		synchronized (interfaces) {
			for (UserInterface u : interfaces)
				if (!u.equals(ui))
					u.onDisplayNameChange(name);
		}
	}
	
	public String getDisplayName() { return name; }
	
	
	
	public void setHostnameAlias(String hostname, String value, UserInterface ui) {
		if (value != null) {
			value = value.trim();
			if(value.equals(""))
				value = null;
		}
		
		Map<String, String> copyOfAliases;
		
		synchronized (aliases) {
			aliases.put(hostname, value);
			copyOfAliases = new HashMap<String, String>(aliases);
		}
		
		synchronized (interfaces) {
			for (UserInterface u : interfaces)
				if (!u.equals(ui))
					u.onAliasesChange(copyOfAliases);
		}
	}
	
	public String getHostnameAlias(String hostname) {
		synchronized (aliases) {
			return aliases.get(hostname);
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public void addInterface(UserInterface ui) {
		if (ui != null) {
			synchronized (interfaces) {
				if (!interfaces.contains(ui))
					interfaces.add(ui);
			}
		}
	}
	
	public void removeInterface(UserInterface ui) {
		synchronized (interfaces) {
			interfaces.remove(ui);
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Prends en charge les messages reçu via le réseau.<br/>
	 * <b>Attention : Cela inclus nos propres messages.</b>
	 * @param m
	 */
	public synchronized void onReceiveMessage(Message message) {
		
		
		ReadOnlyMessage messForInterface = new ReadOnlyMessage(message);
		synchronized (interfaces) {
			for (UserInterface ui : interfaces)
				ui.onReceiveMessage(messForInterface);
		}
		
		
		
		/*
		 * On parcours l'historique de la fin jusqu'au début, pour savoir si on peut fusionner le message reçu avec un message similaire récent (évite les spams)
		 */
		ListIterator<Message> li = messagesHistory.listIterator(messagesHistory.size());
		
		boolean merged = false;
		
		while(li.hasPrevious()) {
			Message m = li.previous();
			if (m.canBeMergedWith(message, TIME_BETWEEN_MESSAGE_FOR_SPAM)) {
				m.addcount();
				merged = true;
			}
		}
		
		if (!merged) {
			synchronized (messagesHistoryLocker) {
				messagesHistory.add(message);
				readOnlyMessageHistory.add(new ReadOnlyMessage(message));
				while (messagesHistory.size()>HISTORY_LENGTH) {
					messagesHistory.remove(0);
					readOnlyMessageHistory.remove(0);
				}
			}
			if (message.hostName != null) {
				synchronized (aliases) {
					if (!aliases.containsKey(message.hostName))
						setHostnameAlias(message.hostName, null, null);
				}
			}
			
		}
		
		
		
		List<ReadOnlyMessage> messages = getMessagesHistory();
		synchronized (interfaces) {
			for (UserInterface ui : interfaces)
				ui.onMessagesHistoryUpdate(messages);
		}
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private synchronized List<ReadOnlyMessage> getMessagesHistory() {
		return Collections.unmodifiableList(readOnlyMessageHistory);
	}
	
	
	
	
	
	
	
	public static class ReadOnlyMessage {
		
		private Message m;
		
		public ReadOnlyMessage(Message mess) {
			m = mess;
		}
		
		public String getMessage() { return m.data; }
		public boolean isRemote() { return getRemoteAddress() != null; }
		public InetAddress getRemoteAddress() { return m.address; }
		public String getRemoteHostName() { return m.hostName; }
		public int getCount() { return m.getCount(); }
		public Date getDate() { return m.date; }
		
	}
	
}
