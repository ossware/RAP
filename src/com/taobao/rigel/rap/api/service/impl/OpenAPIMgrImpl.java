package com.taobao.rigel.rap.api.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.taobao.rigel.rap.api.service.OpenAPIMgr;
import com.taobao.rigel.rap.project.bo.Action;
import com.taobao.rigel.rap.project.bo.Module;
import com.taobao.rigel.rap.project.bo.Page;
import com.taobao.rigel.rap.project.bo.Parameter;
import com.taobao.rigel.rap.project.bo.Project;
import com.taobao.rigel.rap.project.service.ProjectMgr;

public class OpenAPIMgrImpl implements OpenAPIMgr {

	ProjectMgr projectMgr;

	public ProjectMgr getProjectMgr() {
		return projectMgr;
	}

	public void setProjectMgr(ProjectMgr projectMgr) {
		this.projectMgr = projectMgr;
	}

	@Override
	public Object getModel(int projectId, String ver) throws Exception {
        Project project;
        if (ver != null && !ver.isEmpty()) {
            project = projectMgr.getProject(projectId, ver);
        } else {
            project = projectMgr.getProject(projectId);
        }
		if (project == null || project.getId() == 0) {
			throw new Exception("Illegal project id");
		}

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("id", project.getId());
		model.put("name", project.getName());
		model.put("ver", project.getVersion());
		model.put("intro", project.getIntroduction());

		List<Map<String, Object>> moduleList = new ArrayList<Map<String, Object>>();

		for (Module m : project.getModuleList()) {

			Map<String, Object> module = new HashMap<String, Object>();
			module.put("id", m.getId());
			module.put("name", m.getName());
			module.put("intro", m.getIntroduction());

			List<Map<String, Object>> pageList = new ArrayList<Map<String, Object>>();

			for (Page p : m.getPageList()) {
				Map<String, Object> page = new HashMap<String, Object>();
				page.put("id", p.getId());
				page.put("name", p.getName());
				page.put("intro", p.getIntroduction());

				List<Map<String, Object>> interfaceList = new ArrayList<Map<String, Object>>();

				for (Action a : p.getActionList()) {
					Map<String, Object> action = new HashMap<String, Object>();
					action.put("id", a.getId());
					action.put("name", a.getName());
					action.put("desc", a.getDescription());
					action.put("reqType", a.getRequestType());
					action.put("reqUrl", a.getRequestUrl());
					interfaceList.add(action);
				}

				page.put("interfaceList", interfaceList);
				pageList.add(page);
			}

			module.put("pageList", pageList);
			moduleList.add(module);
		}

		model.put("moduleList", moduleList);
		return model;
	}

	@Override
	public Object getSchema(int actionId, Action.TYPE type, String ver, int projectId) {
		Action action;
        if (ver != null && !ver.isEmpty() && projectId > 0) {
            action = projectMgr.getAction(actionId, ver, projectId);
        } else {
            action = projectMgr.getAction(actionId);
        }
		Map<String, Object> schema = new HashMap<String, Object>();

		Set<Parameter> pSet = type == Action.TYPE.REQUEST ? action
				.getRequestParameterList() : action.getResponseParameterList();

		schema.put("type", "object");
		schema.put("$schema", "http://json-schema.org/draft-04/schema");
        schema.put("type", "object");
		schema.put("entity_id", actionId);

		Map<String, Object> properties = new HashMap<String, Object>();

		for (Parameter p : pSet) {
			properties.put(p.getIdentifierWithoutMockjsRule(), generateJSONSchema(p));
		}

		schema.put("properties", properties);

		return schema;
	}

	private Object generateJSONSchema(Parameter p) {
		Map<String, Object> pMap = new HashMap<String, Object>();
		pMap.put("entity_id", p.getId());
		pMap.put("type", p.getJSONSchemaDataType());
        pMap.put("title", p.getName());
        pMap.put("description", p.getRemarkWithoutMockjsRule());
        String remark = p.getRemark();
        String format = "";
        if (remark != null && remark.contains("@mock=function")) {
            format = "";
        }
        String identifier = p.getIdentifier();
        if (identifier != null && identifier.contains("|") && identifier.indexOf("|") != identifier.length() - 1) {
            format += "" + identifier.substring(identifier.indexOf("|") + 1);
        } else {
            format += "";
        }
        if (remark != null && remark.contains("@mock=")) {
            format += "|" + remark.substring(remark.indexOf("@mock=") + 6);
        } else {
            format += "|";
        }
        pMap.put("iftest", format);
		Set<Parameter> children = p.getParameterList();
		if (children != null && children.size() > 0) {
			Map<String, Object> properties = new HashMap<String, Object>();
			for (Parameter child : children) {
				properties
						.put(child.getIdentifierWithoutMockjsRule(), generateJSONSchema(child));
			}
			pMap.put("properties", properties);
		}
		return pMap;
	}

}
