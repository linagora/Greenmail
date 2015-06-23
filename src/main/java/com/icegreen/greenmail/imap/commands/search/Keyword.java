package com.icegreen.greenmail.imap.commands.search;

import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.store.SimpleStoredMessage;

public class Keyword implements Criteria {

	private final String flag;

	public Keyword(String flag) throws ProtocolException {
		this.flag = flag;
	}
	
	@Override
	public boolean match(SimpleStoredMessage message) {
		return message.getFlags().contains(flag);
	}
}
