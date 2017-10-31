package com.redhat.developers.msa.api_gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class EnvResolver {
    @Autowired
    private Environment environment;

	public String get(String name, String defaultValue) {
        String toReturn = defaultValue;
        String sys = System.getProperty(name);
        String env = environment.getProperty(name);
        if(sys != null) toReturn = sys;
        if(sys == null && env != null) toReturn = env;
        return toReturn;
	}
	
	public int get(String name, int defaultValue) {
		int toReturn = defaultValue;
		Integer	 sys = Integer.getInteger(name);
		String env = environment.getProperty(name);
		if(sys != null) toReturn = sys;
		if(sys == null && env != null) try {
			toReturn = Integer.parseInt(env);
		} catch (NumberFormatException nfe) {
			System.err.printf("can't parse int from env '%s' of name '%s'", env, name);
		}
		return toReturn;
	}
}
