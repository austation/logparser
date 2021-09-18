package au.id.mcterra;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Status {
	public final String roundId;

	public Status(String text) throws Exception {
		JSONParser parser = new JSONParser();
		Object result = parser.parse(text);
		JSONObject json = (JSONObject)result;
		roundId = (String)((JSONObject)json.get("data")).get("round_id");
	}
}
