package com.hxy.sys.service.impl;

import com.hxy.base.common.Constant;
import com.hxy.base.exception.MyException;
import com.hxy.base.page.Page;
import com.hxy.base.page.PageHelper;
import com.hxy.base.utils.MD5;
import com.hxy.base.utils.StringUtils;
import com.hxy.base.utils.Utils;
import com.hxy.dto.UserWindowDto;
import com.hxy.sys.dao.UserDao;
import com.hxy.sys.dao.UserRoleDao;
import com.hxy.sys.entity.OrganEntity;
import com.hxy.sys.entity.UserEntity;
import com.hxy.sys.service.OrganService;
import com.hxy.sys.service.UserService;
import com.hxy.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.registry.infomodel.User;
import java.util.*;


@Service("userService")
public class UserServiceImpl implements UserService {
	@Autowired
	private UserDao userDao;

	@Autowired
    private UserRoleDao userRoleDao;

	@Autowired
    private OrganService organService;
	
	@Override
	public UserEntity queryObject(String id){
        UserEntity userEntity = userDao.queryObject(id);
        return userEntity;
	}
	
	@Override
	public List<UserEntity> queryList(Map<String, Object> map){
		return userDao.queryList(map);
	}

	@Override
	public UserEntity queryByLoginName(String logName, String status) {
		return userDao.queryByLoginName(logName,status);
	}

	@Override
	public int queryTotal(Map<String, Object> map){
		return userDao.queryTotal(map);
	}
	
	@Override
	@Transactional
	public void save(UserEntity user){
		user.setId(Utils.uuid());
        //保存用户与角色关系
        UserEntity currentUser = UserUtils.getCurrentUser();
        user.setBapid(currentUser.getBapid());
        user.setBaid(currentUser.getBaid());
        user.setCreateId(currentUser.getId());
        user.setCreateTime(new Date());
        saveUserRole(user);
        userDao.save(user);
	}
	
	@Override
	public void update(UserEntity user){
	    if(user == null || StringUtils.isEmpty(user.getId())){
	        throw new MyException("更新失败，用户id不能为空!");
        }
        user.setUpdateId(UserUtils.getCurrentUserId());
	    user.setUpdateTime(new Date());
        //保存用户与角色关系
        saveUserRole(user);
		userDao.update(user);
	}

    /**
     * 保存用更新用户公共操作
     * @param user
     */
	public void saveUserRole(UserEntity user){
        //根据部门,查询所属机构
        if (!StringUtils.isEmpty(user.getBaid())){
            OrganEntity organ = organService.queryObject(user.getBaid());
            if(!Constant.OrganType.DEPART.getValue().equals(organ.getType())){
                throw new MyException("只能选择用户所属部门,请重新选择部门");
            }
            String bapid = findBapid(user.getBaid());
            user.setBapid(bapid);
        }
		//先删除用户与角色关系
        if(!StringUtils.isEmpty(user.getId())){
            userRoleDao.delete(user.getId());
            if(user.getRoleIdList().size() == 0){
                return ;
            }
        }
		//保存用户与角色关系
		Map<String, Object> map = new HashMap<>();
		map.put("userId", user.getId());
		map.put("roleIdList", user.getRoleIdList());
		userRoleDao.save(map);
	}

    /**
     * 查询父机构
     * @param organId 组织id
     * @return
     */
    public  String findBapid(String organId){
        OrganEntity organEntity = organService.queryObject(organId);
        //如果组织为机构或者根节点,停止递归,返回机构
        if(organEntity != null && (Constant.OrganType.ORGAN.getValue().equals(organEntity.getType()) || Constant.OrganType.CATALOG.getValue().equals(organEntity.getType()))){
            return organEntity.getId();
        }else{
            return findBapid(organEntity.getParentId());
        }
    }
	
	@Override
	public void delete(String id){
		userDao.delete(id);
	}
	
	@Override
	public void deleteBatch(String[] ids){
		userDao.deleteBatch(ids);
		userRoleDao.deleteBatchByUserId(ids);
	}

	@Override
	public Map<String, List<String>> queryBapidByUserId(String userId) {
        if(StringUtils.isEmpty(userId)){
            throw new MyException("用户id为空，获取用户组织机构权限失败");
        }
	    //用户对应的机构id,
        List<Map<String, Object>> maps = userDao.queryOrganIdByUserId(userId, Constant.OrganType.ORGAN.getValue());
        Map<String,List<String>> bapidMap = new HashMap<>();
        for (Map<String,Object> map:maps){
            String bapId =(String) map.get("organId");
            String roleId =(String) map.get("roleId");
            //第一次添加角色id Key
            if(!bapidMap.containsKey(roleId)){
                List<String> tempList = new ArrayList<>();
                tempList.add(bapId);
                bapidMap.put(roleId,tempList);
            }else {
                bapidMap.get(roleId).add(bapId);
            }
        }
        return bapidMap;
	}

	@Override
	public Map<String, List<String>> queryBaidByUserId(String userId) {
        if(StringUtils.isEmpty(userId)){
            throw new MyException("用户id为空，获取用户组织机构权限失败");
        }
        //用户对应的部门id,
        List<Map<String, Object>> maps = userDao.queryOrganIdByUserId(userId, Constant.OrganType.DEPART.getValue());
        Map<String,List<String>> baidMap = new HashMap<>();
        for (Map<String,Object> map:maps){
            String baId =(String) map.get("organId");
            String roleId =(String) map.get("roleId");
            //第一次添加角色id Key
            if(!baidMap.containsKey(roleId)){
                List<String> tempList = new ArrayList<>();
                tempList.add(baId);
                baidMap.put(roleId,tempList);
            }else {
                baidMap.get(roleId).add(baId);
            }
        }
        return baidMap;
	}

    @Override
    public List<String> queryBapidByUserIdArray(String userId) {
	    if(StringUtils.isEmpty(userId)){
	        throw new MyException("用户id为空，获取用户组织机构权限失败");
        }
        return userDao.queryOrganIdByUserIdArray(userId, Constant.OrganType.DEPART.getValue());
    }

    @Override
    public List<String> queryBaidByUserIdArray(String userId) {
        if(StringUtils.isEmpty(userId)){
            throw new MyException("用户id为空，获取用户组织机构权限失败");
        }
        return userDao.queryOrganIdByUserIdArray(userId, Constant.OrganType.ORGAN.getValue());
    }

    @Override
	public Page<UserWindowDto> findPage(UserWindowDto userWindowDto, int pageNum) {
		PageHelper.startPage(pageNum, Constant.pageSize);
		userDao.queryListByBean(userWindowDto);
		return PageHelper.endPage();
	}

    @Override
    public int updatePassword(UserEntity user) {
	    if(user == null){
	        throw new MyException("用户信息不能为空!");
        }
        if(StringUtils.isEmpty(user.getNewPassWord())){
	        throw new MyException("新密码不能为空");
        }
        String vialPassWord = MD5.MD5Encode(user.getPassWord());
        UserEntity currentUser = UserUtils.getCurrentUser();
        if(!vialPassWord.equals(currentUser.getPassWord())){
            throw new MyException("密码不正确");
        }
        Map<String,Object> params = new HashMap<>();
        params.put("id",currentUser.getId());
        params.put("passWord",MD5.MD5Encode((user.getNewPassWord())));
        return userDao.updatePassword(params);
    }
}
