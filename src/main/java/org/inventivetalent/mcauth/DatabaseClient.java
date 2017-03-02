package org.inventivetalent.mcauth;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import org.jongo.Jongo;

import java.io.IOException;
import java.util.Collections;

public class DatabaseClient {

	private String          dbName;
	private String          host;
	private int             port;
	private MongoCredential credential;

	private MongoClient   mongoClient;
	private MongoDatabase mongoDatabase;

	public DatabaseClient(String dbName, String host, int port, String user, char[] pass, String authDatabase) {
		this.dbName = dbName;
		this.host = host;
		this.port = port;
		this.credential = MongoCredential.createScramSha1Credential(user, authDatabase, pass);
	}

	public int collectionCount() {
		int c = 0;
		for (String ignored : db().listCollectionNames()) {
			c++;
		}
		return c;
	}

	public ServerAddress connect(int timeout) throws IOException {
		if (mongoClient == null) {
			System.out.println("Connecting to MongoDB " + this.host + ":" + this.port + "...");
			mongoClient = new MongoClient(new ServerAddress(this.host, this.port), Collections.singletonList(this.credential), MongoClientOptions.builder().connectTimeout(timeout).build());
		}
		return mongoClient.getAddress();
	}

	public void disconnect() throws IOException {
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	public MongoDatabase db() {
		if (mongoDatabase == null) {
			System.out.println("Initializing database '" + dbName + "'");
			mongoDatabase = mongoClient.getDatabase(dbName);
		}
		return mongoDatabase;
	}

	public Jongo jongo() {
		return new Jongo(mongoClient.getDB(dbName));
	}

}
