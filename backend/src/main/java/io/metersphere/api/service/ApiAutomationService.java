package io.metersphere.api.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import io.metersphere.api.dto.APIReportResult;
import io.metersphere.api.dto.automation.ApiScenarioDTO;
import io.metersphere.api.dto.automation.ApiScenarioRequest;
import io.metersphere.api.dto.automation.SaveApiScenarioRequest;
import io.metersphere.api.dto.automation.ScenarioStatus;
import io.metersphere.api.dto.definition.RunDefinitionRequest;
import io.metersphere.api.dto.scenario.environment.EnvironmentConfig;
import io.metersphere.api.jmeter.JMeterService;
import io.metersphere.base.domain.*;
import io.metersphere.base.mapper.ApiScenarioMapper;
import io.metersphere.base.mapper.ApiTagMapper;
import io.metersphere.base.mapper.ext.ExtApiScenarioMapper;
import io.metersphere.commons.constants.APITestStatus;
import io.metersphere.commons.constants.ApiRunMode;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.i18n.Translator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jorphan.collections.HashTree;
import org.aspectj.util.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApiAutomationService {
    @Resource
    private ApiScenarioMapper apiScenarioMapper;
    @Resource
    private ExtApiScenarioMapper extApiScenarioMapper;
    @Resource
    private ApiTagMapper apiTagMapper;
    @Resource
    private JMeterService jMeterService;
    @Resource
    private ApiTestEnvironmentService environmentService;
    @Resource
    private ApiScenarioReportService apiReportService;

    private static final String BODY_FILE_DIR = "/opt/metersphere/data/body";

    public List<ApiScenarioDTO> list(ApiScenarioRequest request) {
        ApiTagExample example = new ApiTagExample();
        example.createCriteria().andProjectIdEqualTo(request.getProjectId());
        List<ApiTag> tags = apiTagMapper.selectByExample(example);
        Map<String, String> tagMap = tags.stream().collect(Collectors.toMap(ApiTag::getId, ApiTag::getName));
        List<ApiScenarioDTO> list = extApiScenarioMapper.list(request);
        Gson gs = new Gson();
        list.forEach(item -> {
            if (item.getTagId() != null) {
                StringBuilder buf = new StringBuilder();
                gs.fromJson(item.getTagId(), List.class).forEach(t -> {
                    buf.append(tagMap.get(t));
                    buf.append(",");
                });
                if (buf != null && buf.length() > 0) {
                    item.setTagName(buf.toString().substring(0, buf.toString().length() - 1));
                }
            }
        });
        return list;
    }

    public void deleteByIds(List<String> nodeIds) {
        ApiScenarioExample example = new ApiScenarioExample();
        example.createCriteria().andApiScenarioModuleIdIn(nodeIds);
        apiScenarioMapper.deleteByExample(example);
    }

    public void create(SaveApiScenarioRequest request) {
        checkNameExist(request);
        final ApiScenario scenario = new ApiScenario();
        scenario.setId(request.getId());
        scenario.setName(request.getName());
        scenario.setProjectId(request.getProjectId());
        scenario.setTagId(request.getTagId());
        scenario.setApiScenarioModuleId(request.getApiScenarioModuleId());
        scenario.setModulePath(request.getModulePath());
        scenario.setLevel(request.getLevel());
        scenario.setFollowPeople(request.getFollowPeople());
        scenario.setPrincipal(request.getPrincipal());
        scenario.setStepTotal(request.getStepTotal());
        scenario.setScenarioDefinition(request.getScenarioDefinition());
        scenario.setCreateTime(System.currentTimeMillis());
        scenario.setUpdateTime(System.currentTimeMillis());
        if (StringUtils.isNotEmpty(request.getStatus())) {
            scenario.setStatus(request.getStatus());
        } else {
            scenario.setStatus(ScenarioStatus.Underway.name());
        }
        if (request.getUserId() == null) {
            scenario.setUserId(SessionUtils.getUserId());
        } else {
            scenario.setUserId(request.getUserId());
        }
        scenario.setDescription(request.getDescription());
        apiScenarioMapper.insert(scenario);
    }

    public void update(SaveApiScenarioRequest request) {
        checkNameExist(request);
        final ApiScenario scenario = new ApiScenario();
        scenario.setId(request.getId());
        scenario.setName(request.getName());
        scenario.setProjectId(request.getProjectId());
        scenario.setTagId(request.getTagId());
        scenario.setApiScenarioModuleId(request.getApiScenarioModuleId());
        scenario.setModulePath(request.getModulePath());
        scenario.setLevel(request.getLevel());
        scenario.setFollowPeople(request.getFollowPeople());
        scenario.setPrincipal(request.getPrincipal());
        scenario.setStepTotal(request.getStepTotal());
        scenario.setScenarioDefinition(request.getScenarioDefinition());
        scenario.setUpdateTime(System.currentTimeMillis());
        if (StringUtils.isNotEmpty(request.getStatus())) {
            scenario.setStatus(request.getStatus());
        } else {
            scenario.setStatus(ScenarioStatus.Underway.name());
        }
        scenario.setUserId(request.getUserId());
        scenario.setDescription(request.getDescription());
        apiScenarioMapper.updateByPrimaryKeySelective(scenario);
    }

    public void delete(String id) {
        apiScenarioMapper.deleteByPrimaryKey(id);
    }

    public void deleteBatch(List<String> ids) {
        ApiScenarioExample example = new ApiScenarioExample();
        example.createCriteria().andIdIn(ids);
        apiScenarioMapper.deleteByExample(example);
    }

    public void removeToGc(List<String> ids) {
        ApiScenario record = new ApiScenario();
        record.setStatus(ScenarioStatus.Trash.name());
        ApiScenarioExample example = new ApiScenarioExample();
        example.createCriteria().andIdIn(ids);
        apiScenarioMapper.updateByExampleSelective(record, example);
    }

    private void checkNameExist(SaveApiScenarioRequest request) {
        ApiScenarioExample example = new ApiScenarioExample();
        example.createCriteria().andNameEqualTo(request.getName()).andProjectIdEqualTo(request.getProjectId()).andIdNotEqualTo(request.getId());
        if (apiScenarioMapper.countByExample(example) > 0) {
            MSException.throwException(Translator.get("automation_name_already_exists"));
        }
    }

    public ApiScenario getApiScenario(String id) {
        return apiScenarioMapper.selectByPrimaryKey(id);
    }

    public List<ApiScenario> getApiScenarios(List<String> ids) {
        if (CollectionUtils.isNotEmpty(ids)) {
            return extApiScenarioMapper.selectIds(ids);
        }
        return new ArrayList<>();
    }

    private void createBodyFiles(List<String> bodyUploadIds, List<MultipartFile> bodyFiles) {
        if (!bodyUploadIds.isEmpty()) {
            File testDir = new File(BODY_FILE_DIR);
            if (!testDir.exists()) {
                testDir.mkdirs();
            }
            for (int i = 0; i < bodyUploadIds.size(); i++) {
                MultipartFile item = bodyFiles.get(i);
                File file = new File(BODY_FILE_DIR + "/" + bodyUploadIds.get(i) + "_" + item.getOriginalFilename());
                try (InputStream in = item.getInputStream(); OutputStream out = new FileOutputStream(file)) {
                    file.createNewFile();
                    FileUtil.copyStream(in, out);
                } catch (IOException e) {
                    LogUtil.error(e);
                    MSException.throwException(Translator.get("upload_fail"));
                }
            }
        }
    }

    public void deleteTag(String id) {
        List<ApiScenario> list = extApiScenarioMapper.selectByTagId(id);
        if (!list.isEmpty()) {
            Gson gs = new Gson();
            list.forEach(item -> {
                List<String> tagIds = gs.fromJson(item.getTagId(), List.class);
                tagIds.remove(id);
                item.setTagId(JSON.toJSONString(tagIds));
                apiScenarioMapper.updateByPrimaryKeySelective(item);
            });
        }
    }

    /**
     * 场景测试执行
     *
     * @param request
     * @param bodyFiles
     * @return
     */
    public String run(RunDefinitionRequest request, List<MultipartFile> bodyFiles) {
        List<String> bodyUploadIds = new ArrayList<>(request.getBodyUploadIds());
        createBodyFiles(bodyUploadIds, bodyFiles);
        EnvironmentConfig config = null;
        if (request.getEnvironmentId() != null) {
            ApiTestEnvironmentWithBLOBs environment = environmentService.get(request.getEnvironmentId());
            config = JSONObject.parseObject(environment.getConfig(), EnvironmentConfig.class);
        }
        HashTree hashTree = request.getTestElement().generateHashTree(config);
        request.getTestElement().getJmx(hashTree);

        // 调用执行方法
        jMeterService.runDefinition(request.getId(), hashTree, request.getReportId(), ApiRunMode.SCENARIO.name());
        APIReportResult report = new APIReportResult();
        report.setId(UUID.randomUUID().toString());
        report.setTestId(request.getReportId());
        report.setName("RUN");
        report.setTriggerMode(null);
        report.setCreateTime(System.currentTimeMillis());
        report.setUpdateTime(System.currentTimeMillis());
        report.setStatus(APITestStatus.Running.name());
        report.setUserId(SessionUtils.getUserId());
        apiReportService.addResult(report);
        return request.getId();
    }

}
