package com.hxy.sys.controller;

import java.util.List;
import java.util.Map;

import com.hxy.base.utils.MD5;
import com.hxy.sys.entity.UserEntity;
import com.hxy.sys.service.UserRoleService;
import com.hxy.sys.service.UserService;
import com.hxy.utils.ShiroUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.hxy.base.utils.PageUtils;
import com.hxy.base.utils.Query;
import com.hxy.base.utils.Result;


/**
 * 系统用户表
 * 
 * @author hxy
 * @email huangxianyuan@gmail.com
 * @date 2017-05-03 09:41:38
 */
@RestController
@RequestMapping("sys/user")
public class UserController extends BaseController{
	@Autowired
	private UserService userService;
	@Autowired
	private UserRoleService userRoleService;
	
	/**
	 * 列表
	 */
	@RequestMapping("/list")
	@RequiresPermissions("sys:user:list")
	public Result list(@RequestParam Map<String, Object> params){
		//查询列表数据
        Query query = new Query(params);

		List<UserEntity> userList = userService.queryList(query);
		int total = userService.queryTotal(query);
		
		PageUtils pageUtil = new PageUtils(userList, total, query.getLimit(), query.getPage());
		
		return Result.ok().put("page", pageUtil);
	}
	
	
	/**
	 * 信息
	 */
	@RequestMapping("/info/{id}")
	@RequiresPermissions("sys:user:info")
	public Result info(@PathVariable("id") String id){
		UserEntity user = userService.queryObject(id);
		if(user != null){
			user.setPassWord("");
			user.setRoleIdList(userRoleService.queryRoleIdList(user.getId()));
		}
		return Result.ok().put("user", user);
	}

	/**
	 *
	 * 主页用户信息
	 */
	@RequestMapping("/info")
	public Result info(){
		UserEntity user = userService.queryObject(ShiroUtils.getUserId());
		return Result.ok().put("user", user);
	}
	
	/**
	 * 保存
	 */
	@RequestMapping("/save")
	@RequiresPermissions("sys:user:update")
	public Result save(@RequestBody UserEntity user){
		if(StringUtils.isNotEmpty(user.getPassWord())){
			user.setPassWord(MD5.MD5Encode(user.getPassWord()));
		}
		userService.save(user);
		return Result.ok();
	}
	
	/**
	 * 修改
	 */
	@RequestMapping(value = "/update")
	@RequiresPermissions("sys:user:update")
	public Result update(@RequestBody UserEntity user){
		user.setPassWord(null);
		userService.update(user);
		
		return Result.ok();
	}

	/**
	 * 修改密码
	 */
	@RequestMapping(value = "/updatePassword",method = RequestMethod.POST)
	@RequiresPermissions("sys:user:updatePassword")
	public Result updatePassword(UserEntity user){
        int i = userService.updatePassword(user);
        if(i<1){
            return Result.error("更改密码失败");
        }
        return Result.ok("更改密码成功");
	}

	
	/**
	 * 删除
	 */
	@RequestMapping("/delete")
	@RequiresPermissions("sys:user:delete")
	public Result delete(@RequestBody String[] ids){
		userService.deleteBatch(ids);
		return Result.ok();
	}
	
}
