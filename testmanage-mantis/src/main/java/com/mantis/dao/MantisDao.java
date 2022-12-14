package com.mantis.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.mantis.entity.MantisIssue;

@Repository
public interface MantisDao {

	List<MantisIssue> queryALLissue(@Param("start_page") Integer start_page
			, @Param("page_size") Integer page_size
			, @Param("reporter") String reporter
			, @Param("handler") String handler
			, @Param("status") String status
			, @Param("workNo") String workNo
			, @Param("startDate") String startDate
			, @Param("endDate") String endDate
			, @Param("env") String env
			, @Param("includeCloseFlag") String includeCloseFlag
			, @Param("id") String id
			, @Param("groupIds") List<String> groupIds
			, @Param("redmine_id") String redmine_id
			, @Param("app_name") String app_name
			, @Param("project_name") String project_name
			, @Param("task_no") String task_no
			, @Param("openTimes") String openTimes,
									String auditFlag) throws Exception;

	List<Map<String,String>> countReporterSum(@Param("startDate")String startDate, 
			@Param("endDate")String endDate
			,@Param("env")String env
	       ) throws Exception;
	
	int count(@Param("reporter") String reporter
            , @Param("handler") String handler
            , @Param("status") String status
            , @Param("workNo") String workNo
            , @Param("startDate") String startDate
            , @Param("endDate") String endDate
            , @Param("env") String env
            , @Param("includeCloseFlag") String includeCloseFlag
            , @Param("id") String id
            , @Param("groupIds") List<String> groupIds
            , @Param("redmine_id") String redmine_id
            , @Param("app_name") String app_name
            , @Param("project_name") String project_name
            , @Param("task_no") String task_no
            , @Param("openTimes") String openTimes,
              String auditFlag) throws Exception;

	MantisIssue queryIssueDetail(@Param("id")String id) throws Exception;

	List<MantisIssue> queryIssueByPlanResultId(@Param("id")String id,@Param("env")String env) throws Exception;

	List<MantisIssue> exportMantis(@Param("reporter") String reporter
			, @Param("handler") String handler
			, @Param("status") String status
			, @Param("workNo") String workNo
			, @Param("startDate") String startDate
			, @Param("endDate") String endDate
			, @Param("includeCloseFlag") String includeCloseFlag
			, @Param("env") String env
			, @Param("groupIds") List<String> groupIds
			, @Param("redmine_id") String redmine_id
			, @Param("app_name") String app_name
			, @Param("project_name") String project_name,
								   String auditFlag) throws Exception;


	List<MantisIssue> queryFuserMantis(@Param("start_page")Integer start_page
			,@Param("page_size")Integer page_size
			,@Param("handlers")String handlers
			,@Param("env")String env
	) throws Exception;

	Integer queryFuserMantisCount(@Param("handlers")String handlers,@Param("env")String env);

    List<MantisIssue> queryFuserMantisAll(@Param("handlers")String handlers,@Param("env")String env, @Param("includeCloseFlag")String includeCloseFlag) throws Exception;


	List<Map<String, String>> countWorkOrderSum(@Param("startDate")String startDate, 
			@Param("endDate")String endDate,@Param("env")String env) throws Exception;

	Map<String, Integer> queryWorkOrderIssues(@Param("workNo")String workNo, @Param("startDate")String startDate,
											  @Param("endDate")String endDate, @Param("env")String env) throws Exception;

	Map<String, Integer> queryOrderUnderwayIssues(String workNo) throws Exception;

	List<Map<String, String>> countMantisByWorkNo(@Param("workNo") String workNo) throws Exception;

	Map<String, Integer> queryOrderUnderwayIssues(String workNo,@Param("env")String env) throws Exception;

	List<Map<String, String>> countMantisByWorkNo(@Param("workNo") String workNo,@Param("env")String env) throws Exception;

	Map<String, String> queryIssueByTimeUser(@Param("workNo")String workNo,
											 @Param("userEnName")String userEnName,
											 @Param("startDate")String startDate,
											 @Param("endDate")String endDate,
											 @Param("env")String env
	                                         ) throws Exception;

	List<Map<String, Object>> countIssueDetailByOrderNos(@Param("workNos") String workNos,
														 @Param("startDate") String startDate,
														 @Param("endDate") String endDate,
														 @Param("env") String env,
														 @Param("groupIds") List<String> groupIds) throws Exception;

	List<Map<String, Object>> queryGroupIssueInfo(@Param("groupIds")List<String> groupIds,@Param("env")String env) throws  Exception;

    List<Map<String, String>> qualityReport(@Param("fdevGroupId") String fdevGroupId, @Param("env")String env) throws Exception;

    List<Map<String, String>> reopenIssue(@Param("fdevGroupId") String fdevGroupId, @Param("env")String env) throws Exception;

    List<Map<String, String>> solveTime(@Param("fdevGroupId") String fdevGroupId, @Param("env")String env) throws Exception;

	List<MantisIssue> queryTasksMantis(@Param("taskId")String taskId, @Param("env")String env, @Param("includeCloseFlag")String includeCloseFlag) throws Exception;

	List<MantisIssue> queryTasksMantisPage(@Param("taskId")String taskId, @Param("env")String env, @Param("start_page")Integer start_page, @Param("page_size")Integer page_size) throws Exception;

	Integer countTasksMantisPage(@Param("taskId")String taskId, @Param("env")String env) throws Exception;

    List<Map<String, String>> qualityReport(@Param("fdevGroupId") String fdevGroupId,
                                            @Param("startDate") String startDate,
                                            @Param("endDate") String endDate,
                                            @Param("env")String env
											) throws Exception;

    List<Map<String, String>> reopenIssue(@Param("fdevGroupId") String fdevGroupId,
                                          @Param("startDate") String startDate,
                                          @Param("endDate") String endDate,
                                          @Param("env")String env
										  ) throws Exception;

    List<Map<String, String>> solveTime(@Param("fdevGroupId") String fdevGroupId,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate,
                                        @Param("env")String env
										) throws Exception;

    List<Map<String, String>> qualityReportAll(@Param("env")String env) throws Exception;

    //??????????????????
	int countTotal(@Param("workNo") String workNo,
				   @Param("userNameEn") String userNameEn,
				   @Param("env") String env) throws Exception;

	//????????????
	List<MantisIssue> queryMantis(@Param("workNo") String workNo ,
								  @Param("userNameEn") String userNameEn,
								  @Param("status")   String status ,
								  @Param("env") String env
								 ) throws Exception;

	//??????????????????
	List<MantisIssue> queryMantisByWorkNos(@Param("workNo") String workNo,@Param("env") String env);

	//?????????????????????90?????????
	int updateMantisStatus(@Param("ids") String ids);

	//????????????ids??????????????????
	int updateMantisByTaskIds(@Param("taskId") String taskId,@Param("env") String env);

	List<Integer> queryMantisIdByTaskNo(String taskNo, String env);

	Map<String,Object> queryIssueById(String id);

	/**
	 * ????????????????????????
	 * @param id ????????????
	 * @param auditFlag 0-????????????1-????????????
	 */
    void updateMantisAudit(String id, String auditFlag);

	/**
	 * ????????????????????????
	 * @param id
	 * @return
	 */
	Map<String, String> queryMantisAuditInfo(String id);

	/**
	 * ??????????????????
	 * @param status
	 * @param id
	 */
	void updateMantisStatusById(String status, String id);

	/**
	 * ?????????????????????????????????????????????
	 * @param id
	 * @return
	 */
	Map<String, String> queryReportNameAndSummary(String id);

	/**
	 * ????????????????????????????????????
	 * @param id
	 * @param wantStatus
	 * @param auditReason
	 * @param wantFlawSource
	 */
	void addFieldString(String id, String wantStatus, String auditReason, String wantFlawSource);

	/**
	 * ???????????????????????????
	 * @param id
	 * @param status
	 * @param reason
	 * @param flawSource
	 */
	void updateMantisByAudit(String id, String status, String reason, String flawSource);

	/**
	 * ?????????????????????
	 * @param historyList
	 */
	void addMantisHistory(@Param("historyList") List<Map> historyList);

	/**
	 * ????????????????????????????????????
	 * @param id
	 * @param fieldIds
	 */
    void deleteFieldString(String id, List<String> fieldIds);

	/**
	 * ?????????????????????????????????
	 * @param id
	 * @param fieldIds
	 * @return
	 */
	int queryFieldString(String id, List<String> fieldIds);

	/**
	 * ????????????????????????????????????
	 * @param id
	 * @param wantStatus
	 * @param auditReason
	 * @param wantFlawSource
	 */
	void updateFieldString(String id, String wantStatus, String auditReason, String wantFlawSource);

	/**
	 * ??????????????????????????????????????????
	 * @param id
	 * @param env
	 */
	void setMantisEnv(String id, String env);

	/**
	 * ?????????????????????????????????
	 * @param env
	 * @param time
	 * @return
	 */
	List<Map> queryMantisLastYear(String env, long time);

	/**
	 * ???????????????????????????
	 * @param id
	 * @param fdevGroupId
	 * @param fdevGroupName
	 */
    void updateMantisGroup(long id, String fdevGroupId, String fdevGroupName);

	/**
	 *
	 * @param startTime
	 * @param endTime
	 * @param groupIds
	 * @param env
	 * @return
	 */
    List<Map> countMantisByGroup(Long startTime, Long endTime, List<String> groupIds, String env);

	/**
	 * ????????????????????????????????????????????????????????????
	 * @param startDate
	 * @param endDate
	 * @param userNameEnList
	 * @param env
	 * @return
	 */
	List<Map<String, String>> countReporterSumNew(String startDate, String endDate, List<String> userNameEnList, String env);

	/**
	 * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????
	 * @param workNoList
	 * @param userEnName
	 * @param startDate
	 * @param endDate
	 * @param env
	 * @return
	 */
	List<Map<String, String>> queryIssueByTimeUserNew(List<String> workNoList, String userEnName, String startDate, String endDate, String env);

	List<Long> queryMantisByWorkNo(String workNo);
}
