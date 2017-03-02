package org.inventivetalent.mcauth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import org.bson.Document;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

public class DatabaseParser {

	final static CodecRegistry CODEC_REGISTRY = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry());
	final static DocumentCodec CODEC          = new DocumentCodec(CODEC_REGISTRY, new BsonTypeClassMap());

	public static JsonObject toJson(Document document) {
		if (document == null) { return null; }
		return new JsonParser().parse(document.toJson(CODEC)).getAsJsonObject();
	}

	public static Document toDocument(JsonElement jsonObject) {
		return Document.parse(jsonObject.toString(), CODEC);
	}

}