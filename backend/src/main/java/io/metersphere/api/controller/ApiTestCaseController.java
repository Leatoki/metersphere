package io.metersphere.api.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.metersphere.api.dto.ApiCaseBatchRequest;
import io.metersphere.api.dto.definition.ApiTestCaseDTO;
import io.metersphere.api.dto.definition.ApiTestCaseRequest;
import io.metersphere.api.dto.definition.ApiTestCaseResult;
import io.metersphere.api.dto.definition.SaveApiTestCaseRequest;
import io.metersphere.api.service.ApiTestCaseService;
import io.metersphere.base.domain.ApiTestCaseWithBLOBs;
import io.metersphere.commons.constants.RoleConstants;
import io.metersphere.commons.utils.PageUtils;
import io.metersphere.commons.utils.Pager;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.track.request.testcase.ApiCaseRelevanceRequest;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping(value = "/api/testcase")
@RequiresRoles(value = {RoleConstants.TEST_MANAGER, RoleConstants.TEST_USER, RoleConstants.TEST_VIEWER}, logical = Logical.OR)
public class ApiTestCaseController {

    @Resource
    private ApiTestCaseService apiTestCaseService;

    @PostMapping("/list")
    public List<ApiTestCaseResult> list(@RequestBody ApiTestCaseRequest request) {
        request.setWorkspaceId(SessionUtils.getCurrentWorkspaceId());
        return apiTestCaseService.list(request);
    }

    @PostMapping("/list/{goPage}/{pageSize}")
    public Pager<List<ApiTestCaseDTO>> listSimple(@PathVariable int goPage, @PathVariable int pageSize, @RequestBody ApiTestCaseRequest request) {
        Page<Object> page = PageHelper.startPage(goPage, pageSize, true);
        request.setWorkspaceId(SessionUtils.getCurrentWorkspaceId());
        return PageUtils.setPageInfo(page, apiTestCaseService.listSimple(request));
    }

    @PostMapping(value = "/create", consumes = {"multipart/form-data"})
    public void create(@RequestPart("request") SaveApiTestCaseRequest request, @RequestPart(value = "files") List<MultipartFile> bodyFiles) {
        apiTestCaseService.create(request, bodyFiles);
    }

    @PostMapping(value = "/update", consumes = {"multipart/form-data"})
    public void update(@RequestPart("request") SaveApiTestCaseRequest request, @RequestPart(value = "files") List<MultipartFile> bodyFiles) {
        apiTestCaseService.update(request, bodyFiles);
    }

    @GetMapping("/delete/{id}")
    public void delete(@PathVariable String id) {
        apiTestCaseService.delete(id);
    }

    @PostMapping("/removeToGc")
    public void removeToGc(@RequestBody List<String> ids) {
        apiTestCaseService.removeToGc(ids);
    }

    @GetMapping("/get/{id}")
    public ApiTestCaseWithBLOBs get(@PathVariable String id) {
        return apiTestCaseService.get(id);
    }

    @PostMapping("/batch/edit")
    @RequiresRoles(value = {RoleConstants.TEST_USER, RoleConstants.TEST_MANAGER}, logical = Logical.OR)
    public void editApiBath(@RequestBody ApiCaseBatchRequest request) {
        apiTestCaseService.editApiBath(request);
    }

    @PostMapping("/deleteBatch")
    public void deleteBatch(@RequestBody List<String> ids) {
        apiTestCaseService.deleteBatch(ids);
    }

    @PostMapping("/relevance")
    public void testPlanRelevance(@RequestBody ApiCaseRelevanceRequest request) {
        apiTestCaseService.relevanceByCase(request);
    }
}