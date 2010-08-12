package org.openmrs.module.htmlformflowsheet.web.dwr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformflowsheet.web.SingleHtmlFormPatientChartTab;
import org.openmrs.module.htmlformflowsheet.web.SingleHtmlFormPatientChartTab.Which;
import org.openmrs.module.htmlformflowsheet.web.util.HtmlFormFlowsheetUtil;

public class HtmlFormFlowsheetDWR {

    protected final Log log = LogFactory.getLog(getClass());
    
    public boolean voidEncounter(String encIdSt, Integer formId){
        try{
            EncounterService es = Context.getEncounterService();
            Integer encId = Integer.valueOf(encIdSt);

                //first void all schema obs in encounter
                Encounter enc = es.getEncounter(encId);
                Form form = Context.getFormService().getForm(formId);
                Set<Concept> concepts = HtmlFormFlowsheetUtil.getAllConceptsUsedInHtmlForm(form);
               
                for (Obs o : enc.getAllObs()){
                    for (Concept c : concepts){
                        if (o.getConcept().getConceptId().equals(c.getConceptId())){
                            o.setVoided(true);
                            o.setVoidedBy(Context.getAuthenticatedUser());
                            o.setVoidReason("voided from htmlformflowsheet");
                            o.setDateVoided(new Date());
                        } 
                    }    
                }
                boolean allObsAreVoided = true;
                for (Obs o : enc.getAllObs()){
                    if (!o.isVoided()){
                        allObsAreVoided = false;
                        break;
                    }
                }
                if (allObsAreVoided){
                    //log.info("Voiding Encounter");
                    es.voidEncounter(enc, "mdrtb Cat-IV");
                } else {
                    //log.info("Saving Encounter because there are still unvoided obs.");
                    es.saveEncounter(enc);
                }     
        } catch (Exception ex){
            return false;
        }
        return true;
    }
    
    
    public Integer getNewEncounterId(String whichEnc, Integer formId, Integer patientId){
            Integer ret = 0;
            try {
                List<Encounter> encs = Context.getEncounterService().getEncountersByPatientId(patientId);
                List<Encounter> byForm = new ArrayList<Encounter>();
                SingleHtmlFormPatientChartTab.Which which = SingleHtmlFormPatientChartTab.Which.valueOf(whichEnc);
                for (Encounter enc : encs){
                    if (enc.getForm() != null && enc.getForm().getFormId().equals(formId))
                        byForm.add(enc);
                }
                if (byForm.size() > 0){
                    if (which == Which.FIRST){
                        return byForm.get(0).getEncounterId();
                    } else {
                        return byForm.get(byForm.size()-1).getEncounterId();
                    }
                }
            } catch (Exception ex){
                return ret;
            }
            return ret;
    }
    
    public List<HtmlFormFlowsheetDWREncounterObj> getAllEncsByPatientAndEncType(Integer patientId, Integer encTypeId){
        List<HtmlFormFlowsheetDWREncounterObj> ret = new ArrayList<HtmlFormFlowsheetDWREncounterObj>();
        EncounterType et = Context.getEncounterService().getEncounterType(encTypeId);
        Patient p = Context.getPatientService().getPatient(patientId);
        List<Encounter> eList = Context.getEncounterService().getEncounters(p, null, null, null, null, Collections.singletonList(et), null, false);
        for (Encounter e : eList){
            HtmlFormFlowsheetDWREncounterObj h = new HtmlFormFlowsheetDWREncounterObj(e);
            ret.add(h);
        }
        return ret;
    }
    
    
}
