package org.maksim.chakur.servlet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class UserListTest {
    private User[] users = new User[8];

    @Before
    public void setUp() throws Exception {
        users[0] = new User("Евгений", "1","client");
        users[1] = new User("Алина", null,"client");
        users[2] = new User("Марина", "123",null);
        users[3] = new User("Евгений", "1","client");
        users[4] = new User(" Антон","1","client");
        users[5] = new User("Кристина ","12","client");
        users[6] = new User("David","123","client");
        users[7] = new User("Kate","","agent");
    }

    @Test
    public void findUser() {
        ArrayList<User> expected = new ArrayList<>();

        HashMap<String, User> usersMap = new HashMap<>();
        ArrayList<User> actual = new ArrayList<>();

        for (User user : users) {
            UserList.addUser(user);

            if (!usersMap.containsKey(user.getName()) && !user.getName().equals("") && user.getName().matches("^\\S+\\s*\\S+$") &&
                    user.getPassword() != null && !user.getPassword().equals("") && user.getCharacter() != null) {
                usersMap.put(user.getName(), user);
            }
        }

        for (int i = 0; i < users.length; i++) {
            expected.add(UserList.findUser(users[i].getName()));

            actual.add(usersMap.get(users[i].getName()));
        }

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void addUser() {
        ArrayList<Boolean> expected = new ArrayList<>();

        ArrayList<Boolean> actual = new ArrayList<>();
        HashSet<String> namesSet = new HashSet<>();

        for (User user : users) {
            expected.add(UserList.addUser(user));

            if (namesSet.contains(user.getName())) {
                actual.add(false);
            } else {
                if (!user.getName().equals("") && user.getName().matches("^\\S+\\s*\\S+$") &&
                        user.getPassword() != null && !user.getPassword().equals("") && user.getCharacter() != null) {
                    namesSet.add(user.getName());
                    actual.add(true);
                } else {
                    actual.add(false);
                }
            }
        }
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getUsers_NO_NULL() {
        HashMap<String, User> usersMap = UserList.getUsers();
        Assert.assertNotNull(usersMap);
    }
}