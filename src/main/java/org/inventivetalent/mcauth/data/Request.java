package org.inventivetalent.mcauth.data;

import lombok.Data;

import java.util.Date;

@Data
public class Request {

	String _id;
	String request_id;
	String request_ip;
	String username;
	Status status;
	String token;
	long   tokenTime;
	Date   created;

}
