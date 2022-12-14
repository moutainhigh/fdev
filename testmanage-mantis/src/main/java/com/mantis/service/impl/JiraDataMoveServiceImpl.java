package com.mantis.service.impl;

import com.mantis.dict.Dict;
import com.mantis.entity.JiraMantisMapping;
import com.mantis.service.JiraDataMoveService;
import com.mantis.service.UserService;
import com.mantis.util.MantisRestTemplate;
import com.mantis.util.Utils;
import com.test.testmanagecommon.util.Util;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

@Service
@RefreshScope
public class JiraDataMoveServiceImpl implements JiraDataMoveService {
	@Autowired
	private UserService userService;
	@Autowired
	private MantisRestTemplate restTemplate;
	@Value("${manits.add.issue.url}")
	private String manits_issue_url;
	@Value("${manits.admin.token}")
	private String adminToken;
	
	@Override
	public void jiraDateMove() throws Exception {
		List<JiraMantisMapping> list = new ArrayList<>();
		File file = new File("C:\\Users\\T-lizy\\Desktop\\jiradata.xlsx");
		List<Map> userlist = userService.queryUserByNameCn();
		try(InputStream in = new FileInputStream(file);
			){
			Workbook workbook = new XSSFWorkbook(in);
			Sheet sheet = workbook.getSheetAt(0);
			sheet.getRowSumsBelow();
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (!Utils.isEmpty(row)) {
					JiraMantisMapping jiraMantisMapping = new JiraMantisMapping();
					System.out.println("============"+i);
					Cell summary = row.getCell(1);
					Cell status = row.getCell(2);
					Cell priority = row.getCell(3);
					Cell handler = row.getCell(4);
					Cell reporter = row.getCell(5);
					Cell description = row.getCell(6);
					Cell plan_fix_date = row.getCell(7);
					Cell developer = row.getCell(8);
					Cell reason = row.getCell(9);
					Cell system_version = row.getCell(10);
					Cell flaw_source = row.getCell(11);
					Cell severity = row.getCell(12);
					Cell flaw_type = row.getCell(13);
					Cell stage = row.getCell(14);
					Cell redmine_id = row.getCell(15);
					Cell date_submitted = row.getCell(16);
					jiraMantisMapping.setWorkNo("Jira00000001");
					jiraMantisMapping.setProject_id(19);
					jiraMantisMapping.setSummary(readCell(summary).toString());
					jiraMantisMapping.setStatus(readCell(status).toString());
					jiraMantisMapping.setPriority(readCell(priority).toString());
					jiraMantisMapping.setHandler(readCell(handler).toString());
					jiraMantisMapping.setReporter(readCell(reporter).toString());
					jiraMantisMapping.setDescription(readCell(description).toString());
					if(!Utils.isEmpty(readCell(plan_fix_date))){
						jiraMantisMapping.setPlan_fix_date((Long)readCell(plan_fix_date));
					}else {
						jiraMantisMapping.setPlan_fix_date(Long.valueOf(0));
					}
					jiraMantisMapping.setDeveloper(readCell(developer).toString());
					jiraMantisMapping.setReason(readCell(reason).toString());
					jiraMantisMapping.setSystem_version(readCell(system_version).toString());
					jiraMantisMapping.setFlaw_source(readCell(flaw_source).toString());
					jiraMantisMapping.setSeverity(readCell(severity).toString());
					jiraMantisMapping.setFlaw_type(readCell(flaw_type).toString());
					jiraMantisMapping.setStage(readCell(stage).toString());
					jiraMantisMapping.setRedmine_id(readCell(redmine_id).toString());
					if(!Utils.isEmpty(readCell(date_submitted))){
						jiraMantisMapping.setDate_submitted((Long)readCell(date_submitted));
					}else {
						jiraMantisMapping.setDate_submitted(Long.valueOf(0));
					}
					jiraMantisMapping = checkData(jiraMantisMapping,userlist);
					list.add(jiraMantisMapping);
				}
			}
			addIssues(list);
		}

	}
	
	public String queryUserNameEn(String userName,List<Map> userlist) throws Exception {
		for (Map map : userlist) {
			String user_name = (String) map.get("user_name");
			if(userName.equals(user_name)) {
				return (String) map.get("user_en_name");
			}
		}
		return "T-fengwy";
	}
	
	public Object readCell(Cell cell) {
		Object value = "";
		if(Utils.isEmpty(cell)) {
			return value;
		}
		switch (cell.getCellType()) {
		case Cell.CELL_TYPE_STRING:
			 value = cell.getStringCellValue();
		break;
		case Cell.CELL_TYPE_NUMERIC:
			 if(HSSFDateUtil.isCellDateFormatted(cell)) {
				 Date date = HSSFDateUtil.getJavaDate(cell.getNumericCellValue());
				 value =  date.getTime()/1000;
			 }else {
				 cell.setCellType(Cell.CELL_TYPE_STRING);
				 value = cell.getStringCellValue();
			 }
		break;	 
		default :
			value = "";
			break;
		}
		return value;
		
	}
	
	public JiraMantisMapping checkData(JiraMantisMapping jira,List<Map> userlist) throws Exception {
		String status = "";
		switch (jira.getStatus()) {
		case "??????":
			status = "10";
		break;
		case "??????":
			status = "20";
		break;	 
		case "????????????":
			status = "30";
		break;
		case "??????":
			status = "40";
		break;
		case "Reopen":
			status = "50";
		break;
		case "??????":
			status = "90";
		break;
		case "??????":
			status = "80";
		break;
		default :
			status = "10";
		}
		jira.setStatus(status);
		
		String priority = "";
		switch (jira.getPriority()) {
		case "??????":
			priority = "10";
		break;
		case "???":
			priority = "20";
		break;	 
		case "???":
			priority = "30";
		break;
		case "???":
			priority = "40";
		break;
		case "??????":
			priority = "50";
		break;
		case "??????":
			priority = "60";
		break;
		default :
			priority = "10";
		}
		jira.setPriority(priority);
		
		String handler = jira.getHandler();
		String[] split = handler.split("\\(");
		handler = split[0];
		String handler_en_name = "";
		if(!Util.isNullOrEmpty(handler)) {
			handler_en_name = queryUserNameEn(handler,userlist);
		}else {
			handler_en_name = "T-fengwy";
		}
		jira.setHandler(handler_en_name);
		
		String reporter = jira.getReporter();
		String[] split2 = reporter.split("\\(");
		reporter = split2[0];
		String reporter_en_name = "";
		if(!Util.isNullOrEmpty(reporter)) {
			reporter_en_name = queryUserNameEn(reporter,userlist);
		}else {
			reporter_en_name = "T-fengwy";
		}
		jira.setReporter(reporter_en_name);
		
		if(Util.isNullOrEmpty(jira.getSystem_version())) {
			jira.setSystem_version("????????????");
		}
		
		if(Util.isNullOrEmpty(jira.getFlaw_source())) {
			jira.setFlaw_source("????????????");
		}
		
		String severity = "";
		switch (jira.getSeverity()) {
		case "?????????":
			severity = "10";
		break;
		case "G4-??????":
			severity = "20";
		break;	 
		case "??????":
			severity = "30";
		break;
		case "G3-??????":
			severity = "40";
		break;
		case "G2-??????":
			severity = "50";
		break;
		case "G1-??????":
			severity = "60";
		break;
		case "G0-??????":
			severity = "70";
		break;
		default :
			severity = "50";
		}
		jira.setSeverity(severity);
		
		String type = "";
		switch (jira.getFlaw_type()) {
		case "????????????":
			type = "????????????";
		break;
		case "????????????":
			type = "????????????";
		break;	 
		case "???????????? ":
			type = "???????????? ";
		break;
		case "????????????":
			type = "????????????";
		break;
		case "????????????":
			type = "????????????";
		break;
		case "????????????":
			type = "????????????";
		break;
		default :
			type = "????????????";
		}
		jira.setFlaw_type(type);
		if(Util.isNullOrEmpty(jira.getStage())) {
			jira.setStage("????????????");
		}	
		return jira;
	}
	
	public void addIssues(List<JiraMantisMapping> issues) {
		int i = 1;
		for (JiraMantisMapping jira : issues) {
			System.out.println("============"+i++);
			String serverity = jira.getSeverity();
			String priority = jira.getPriority();
			String workNo = jira.getWorkNo();
			String summary = jira.getSummary();
			String description = jira.getDescription();
			if(Utils.isEmpty(description)) {
				description = "???";
			}
			Integer project = jira.getProject_id();
			String handler = jira.getHandler();//T-fengwy
			String stage = jira.getStage();//????????????
			String reason = jira.getReason(); //?????????????????? 
			String flaw_source = jira.getFlaw_source(); //????????????
			String system_version = jira.getSystem_version(); //????????????
			String developer = jira.getDeveloper();//????????????
			long plan_fix_date = jira.getPlan_fix_date();//??????????????????????????????
			String redmine_id = jira.getRedmine_id();//????????????????????????
			String flaw_type = jira.getFlaw_type();//????????????
			String function_module = jira.getFunction_module(); //????????????
			long date_submitted = jira.getDate_submitted();
			String reporter = jira.getReporter();
			String status = jira.getStatus();
			function_module = function_module == null ? "" : function_module;
			Map<String,Object> sendMap = new HashMap<String,Object>();
			sendMap.put(Dict.SUMMARY, summary);
			sendMap.put(Dict.DESCRIPTION, description);
			sendMap.put(Dict.ADDITIONAL_INFORMATION, "");
			sendMap.put(Dict.PROJECT, assemblyParamMap(Dict.ID,project));
			sendMap.put(Dict.HANDLER, assemblyParamMap(Dict.NAME,handler));
			sendMap.put(Dict.PRIORITY, assemblyParamMap(Dict.ID,priority));
			sendMap.put(Dict.SEVERITY,assemblyParamMap(Dict.ID,serverity));
			sendMap.put(Dict.CATEGORY, assemblyParamMap(Dict.ID,1));
			sendMap.put(Dict.STICKY, false);
			List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
			list.add(assemblyCustomMap(3,stage));//'????????????'
			list.add(assemblyCustomMap(4,reason));//'??????????????????'
			list.add(assemblyCustomMap(5,flaw_source));//'????????????'
			list.add(assemblyCustomMap(6,system_version));//'????????????'
			list.add(assemblyCustomMap(7,"????????????"));//'????????????'
			list.add(assemblyCustomMap(8,developer));//'????????????'
			list.add(assemblyCustomMap(9, plan_fix_date));//'??????????????????????????????'
			list.add(assemblyCustomMap(10,redmine_id));//'????????????????????????'
			list.add(assemblyCustomMap(11,flaw_type));//'????????????'
			list.add(assemblyCustomMap(12,function_module));//'????????????'
			list.add(assemblyCustomMap(13, workNo));
			list.add(assemblyCustomMap(15,date_submitted));
			list.add(assemblyCustomMap(16,reporter));
			list.add(assemblyCustomMap(17,status));
			sendMap.put(Dict.CUSTOM_FIELDS, list);
			restTemplate.sendPost(manits_issue_url, adminToken, sendMap);
		}
		
	}
	
	public Map<String,Object> assemblyCustomMap(Integer id,Object value) {
		Map<String,Object> customMap = new HashMap<String,Object>();
		Map<String,Integer> custom_item = new HashMap<String,Integer>();
		custom_item.put(Dict.ID, id);
		customMap.put(Dict.FIELD, custom_item);
		customMap.put(Dict.VALUE, value);
		return customMap;
	}
	
	public Map<String,Object> assemblyParamMap(String type ,Object value) {
		Map<String,Object> projectMap = new HashMap<String,Object>();
		projectMap.put(type, value);
		return projectMap;
	}

}
