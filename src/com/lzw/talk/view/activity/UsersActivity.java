package com.lzw.talk.view.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.avos.avoscloud.AVUser;
import com.lzw.talk.R;
import com.lzw.talk.adapter.UserAdapter;
import com.lzw.talk.avobject.User;
import com.lzw.talk.base.App;
import com.lzw.talk.base.C;
import com.lzw.talk.receiver.MsgReceiver;
import com.lzw.talk.service.ChatService;
import com.lzw.talk.service.StatusListener;
import com.lzw.talk.util.NetAsyncTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lzw on 14-7-9.
 */
public class UsersActivity extends Activity implements AdapterView.OnItemClickListener, StatusListener {
  public static final int MENU_SET_NICKNAME = 0;
  private static final int MENU_LOGOUT = 1;
  ListView usersList;
  Activity cxt;
  private UserAdapter userAdapter;
  List<User> users;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.chat_users_layout);
    cxt = this;
    findView();
    setList();
    new GetDataTask(cxt).execute();
    MsgReceiver.registerStatusListener(this);
    registerMyselfToCache();
  }

  private void registerMyselfToCache() {
    User user = User.curUser();
    String userId = user.getObjectId();
    App.registerUserCache(userId, user);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(0, MENU_LOGOUT, 0, R.string.logout);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    int id = item.getItemId();
    if (id == MENU_LOGOUT) {
      AVUser.logOut();
      finish();
    }
    return super.onMenuItemSelected(featureId, item);
  }

  private void findView() {
    usersList = (ListView) findViewById(R.id.contactList);
  }

  private void setList() {
    userAdapter = new UserAdapter(cxt);
    usersList.setAdapter(userAdapter);
    usersList.setOnItemClickListener(this);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    Intent intent = new Intent(cxt, ChatActivity.class);
    User user = (User) parent.getItemAtPosition(position);
    App.chatUser = user;
    startActivity(intent);
  }

  @Override
  public void onStatusOnline(List<String> peerIds) {
    if (users == null) {
      return;
    }
    for (AVUser user : users) {
      if (peerIds.contains(ChatService.getPeerId(user))) {
        user.put(C.ONLINE, true);
      } else {
        user.put(C.ONLINE, false);
      }
    }
    userAdapter.notifyDataSetChanged();
  }

  class GetDataTask extends NetAsyncTask {

    protected GetDataTask(Context cxt) {
      super(cxt);
    }

    @Override
    protected void doInBack() throws Exception {
      users = ChatService.findChatUsers();
      App.registerBatchUserCache(users);
    }

    @Override
    protected void onPost(boolean res) {
      if (res) {
        userAdapter.setUsers(users);
        ChatService.withUsersToWatch(users, true);
        onStatusOnline(new ArrayList<String>(MsgReceiver.onlines));
      }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    MsgReceiver.unregisterSatutsListener();
  }
}