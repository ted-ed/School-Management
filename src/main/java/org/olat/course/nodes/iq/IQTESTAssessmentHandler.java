/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.course.nodes.iq;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.BreadcrumbPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BlankController;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.util.Util;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.assessment.handler.AssessmentConfig;
import org.olat.course.assessment.handler.AssessmentHandler;
import org.olat.course.assessment.handler.NonAssessmentConfig;
import org.olat.course.assessment.ui.tool.AssessmentCourseNodeController;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.IQTESTCourseNode;
import org.olat.course.run.scoring.ScoreCalculator;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.fileresource.types.ImsQTI21Resource;
import org.olat.group.BusinessGroup;
import org.olat.ims.qti.QTI12ResultDetailsController;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.ims.qti21.ui.QTI21AssessmentDetailsController;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.ui.AssessmentToolContainer;
import org.olat.modules.assessment.ui.AssessmentToolSecurityCallback;
import org.olat.repository.RepositoryEntry;
import org.olat.resource.OLATResource;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 20 Aug 2019<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
@Service
public class IQTESTAssessmentHandler implements AssessmentHandler {

	@Override
	public String acceptCourseNodeType() {
		return IQTESTCourseNode.TYPE;
	}

	@Override
	public AssessmentConfig getAssessmentConfig(CourseNode courseNode) {
		if (courseNode instanceof IQTESTCourseNode) {
			IQTESTCourseNode iqtestNode = (IQTESTCourseNode) courseNode;
			return new IQTESTAssessmentConfig(iqtestNode);
		}
		return NonAssessmentConfig.create();
	}

	@Override
	public AssessmentEntry getAssessmentEntry(CourseNode courseNode, UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity assessedIdentity = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		if(getRepositoryEntrySoftKey(courseNode) != null) {
			return am.getAssessmentEntry(courseNode, assessedIdentity);
		}
		return null;
	}
	
	private String getRepositoryEntrySoftKey(CourseNode courseNode) {
		return (String)courseNode.getModuleConfiguration().get(IQEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
	}

	@Override
	public ScoreEvaluation getCalculatedScoreEvaluation(CourseNode courseNode,
			UserCourseEnvironment userCourseEnvironment) {
		return null;
	}

	@Override
	public ScoreCalculator getScoreCalculator(CourseNode courseNode) {
		return null;
	}
	
	@Override
	public Controller getDetailsEditController(UserRequest ureq, WindowControl wControl, BreadcrumbPanel stackPanel,
			CourseNode courseNode, UserCourseEnvironment coachCourseEnv, UserCourseEnvironment assessedUserCourseEnv) {
		Controller detailsCtrl = null;
		RepositoryEntry ref = courseNode.getReferencedRepositoryEntry();
		if(ref != null) {
			OLATResource resource = ref.getOlatResource();
			Long courseResourceableId = assessedUserCourseEnv.getCourseEnvironment().getCourseResourceableId();
			Identity assessedIdentity = assessedUserCourseEnv.getIdentityEnvironment().getIdentity();
			
			if(ImsQTI21Resource.TYPE_NAME.equals(resource.getResourceableTypeName())) {
				if (courseNode instanceof IQTESTCourseNode) {
					IQTESTCourseNode iqtestNode = (IQTESTCourseNode)courseNode;
					RepositoryEntry courseEntry = assessedUserCourseEnv.getCourseEnvironment().getCourseGroupManager().getCourseEntry();
					detailsCtrl = new QTI21AssessmentDetailsController(ureq, wControl, (TooledStackedPanel) stackPanel,
							courseEntry, iqtestNode, coachCourseEnv, assessedUserCourseEnv);
				}
			} else if(QTIResourceTypeModule.isOnyxTest(ref.getOlatResource())) {
				Translator trans = Util.createPackageTranslator(IQEditController.class, ureq.getLocale());
				detailsCtrl = MessageUIFactory.createInfoMessage(ureq, wControl, "", trans.translate("error.onyx"));
			} else {
				detailsCtrl = new QTI12ResultDetailsController(ureq, wControl, courseResourceableId,
						courseNode.getIdent(), coachCourseEnv, assessedIdentity, ref,
						AssessmentInstance.QMD_ENTRY_TYPE_ASSESS);
			}	
		}
		return detailsCtrl != null? detailsCtrl: new BlankController(ureq, wControl);
	}

	@Override
	public boolean hasCustomIdentityList() {
		return true;
	}
	
	@Override
	public AssessmentCourseNodeController getIdentityListController(UserRequest ureq, WindowControl wControl,
			TooledStackedPanel stackPanel, CourseNode courseNode, RepositoryEntry courseEntry, BusinessGroup group,
			UserCourseEnvironment coachCourseEnv, AssessmentToolContainer toolContainer,
			AssessmentToolSecurityCallback assessmentCallback) {
		return new IQIdentityListCourseNodeController(ureq, wControl, stackPanel, courseEntry, group, courseNode,
				coachCourseEnv, toolContainer, assessmentCallback);
	}

}
