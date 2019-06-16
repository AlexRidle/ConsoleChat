package org.maksim.chakur.servlet;

import java.util.HashMap;

public class UserList {
	private static HashMap<String, User> users = new HashMap<>();
	public static User findUser(String name){
        return (User)users.get(name);
    }

    public static boolean addUser(User user){
        boolean result = false;
        if ((!users.containsKey(user.getName())) && (!"".equals(user.getName()) && user.getName().matches("^\\S+\\s*\\S+$") &&
                (user.getPassword()!= null) && (!"".equals(user.getPassword())) && (user.getCharacter()!= null))){
            users.put(user.getName(), user);
            result = true;
        }
        return result;
    }

    public static HashMap<String, User> getUsers() {
	    return users;
    }
}
