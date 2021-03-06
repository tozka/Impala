/**
 *    Copyright (C) 2018 Arman Afzal <arman.afzal@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.github.rmanaf.impala.core;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.util.Log;

import com.github.rmanaf.impala.Impala;
import com.github.rmanaf.impala.forms.Submit;
import com.github.rmanaf.impala.generic.Alias;
import com.github.rmanaf.impala.generic.Tuple;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Controller implements IController {

    private ModelRenderer mRenderer;
    private int[] mAnimations;
    private List<StackItem> mStack;
    private Activity mActivity;
    private int mPlaceholder;
    private ControllerOptions mOptions;
    private IOnModelChangedEventListener mListener;
    private String mView;
    private Fragment mFragment;


    public IController config(Activity activity, int placeholder , ControllerOptions options){

        mActivity = activity;
        mPlaceholder = placeholder;

        if(options == null)
            options = new ControllerOptions();

        mOptions = options.$for(this);
        getOptions().onInit();

        return this;

    }


    public void setOnModelChangedEventListener(IOnModelChangedEventListener listener) {

        this.mListener = listener;

    }

    public ModelRenderer getRenderer() {

        return mRenderer;

    }

    public String getSimpleName(){
        return this.getClass().getSimpleName();
    }

    public ModelRenderer setRenderer(ModelRenderer renderer) {

        this.mRenderer = renderer.setWrapper(this);

        return this.mRenderer;

    }

    public int getPlaceholderId() {

        if(mPlaceholder == 0)
            mPlaceholder = Impala.getWrapper();

        if(mPlaceholder == 0)
            Log.e(getSimpleName() , "Wrapper ID is not defined");

        return mPlaceholder;

    }

    public Activity getActivity() {

        if(mActivity == null)
            mActivity = Impala.getActivity();

        if(mActivity == null)
            Log.e(getSimpleName() , "Activity is not defined");

        return mActivity;

    }

    public ControllerOptions getOptions(){

        return mOptions;

    }



    public void view(String page) {

        view(page , null , null ,null, true);

    }

    public void view(String page, Fragment fragment) {

        view(page , null , null ,fragment, true);

    }

    public void view(Model model) {

        view(null , model , null , null,true);

    }

    public void view(Model model , ModelOptions options) {

        view(null , model , options , null, true);

    }

    public void view(String page , Model model) {

        view(page , model , null , null, true);

    }

    public void view(String page , Model model , boolean addToStack) {

        view(page , model , null , null ,addToStack);

    }

    public void view(String page , Model model, ModelOptions options) {

        view(page , model , options , null,true);

    }

    private void view(String page , Model model, ModelOptions options , Fragment fragment , boolean addToStack) {

        if(model != null || fragment != null){

            FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();

            if (mAnimations != null)
                if (mAnimations.length == 2)
                    ft.setCustomAnimations(mAnimations[0], mAnimations[1]);
                else
                    ft.setCustomAnimations(mAnimations[0], mAnimations[1], mAnimations[2], mAnimations[3]);


            // call lifecycle onBeginTransaction hook

            getOptions().onBeginTransaction(ft);

            if (getRenderer() != null  && addToStack) {

                if(getRenderer().getForm().getModel() != null)
                    addToStack(false);
                else if(mFragment != null)
                    addToStack(true);

            }

            if(fragment != null) {

                mFragment = fragment;

                ft.replace(getPlaceholderId(), fragment).commit();

            }

            if(model != null) {

                setRenderer(new ModelRenderer())
                        .render(model, options)
                        .setOnModelChangedEventListener(new IOnModelChangedEventListener() {

                            @Override
                            public void onFieldChanged(BoundData details, Object oldValue) {

                                if (mListener != null)
                                    mListener.onFieldChanged(details, oldValue);

                            }

                            @Override
                            public void onSubmit(Model model) {

                                Controller.this.onSubmit(model);

                                if (mListener != null)
                                    mListener.onSubmit(model);

                            }

                            @Override
                            public void onCollectionItemSelected(Model model, Object item) {

                                if (mListener != null)
                                    mListener.onCollectionItemSelected(model, item);

                            }

                        });

                ft.replace(getPlaceholderId(), getRenderer()).commit();

            }

            if(page != null)
                mView = page;


            return;

        }


        for (Method m : getClass().getDeclaredMethods()) {

            if (!Utilities.methodHasName(m, page, false)) {

                continue;

            }

            try {

                if (!m.isAnnotationPresent(Submit.class) && m.getParameterTypes().length == 0) {

                    mView = page;

                    m.invoke(this);

                    break;

                }

            } catch (Exception e) {

                e.printStackTrace();

                break;

            }

        }

    }



    public boolean popBackStack() {

        StackItem stack = popStack();

        if (stack != null) {

            view(stack.page , stack.model, stack.model.getOptions() , stack.fragment , false);

        }

        return stack != null;

    }

    public void setAnimations(int a1, int a2) {
        mAnimations = new int[]{a1, a2};
    }

    public void setAnimations(int a1, int a2, int a3, int a4) {
        mAnimations = new int[]{a1, a2, a3, a4};
    }

    public void clearAnimations() {

        mAnimations = null;

    }

    public void onSubmit(Model model) {

        // calling associated submit method

        for (Method m : getClass().getDeclaredMethods()) {

            Type[] params = m.getGenericParameterTypes();

            if (!m.isAnnotationPresent(Submit.class) || params.length == 0)
                continue;

            try {

                String submitVal = m.getAnnotation(Submit.class).value().toLowerCase();

                if ((!submitVal.equals("") && submitVal.equals(mView.toLowerCase()) ||
                    ( submitVal.equals("") && Utilities.methodHasName(m , mView , false))) &&
                      params[0] == model.getClass()) {

                    m.invoke(this, model);

                    return;

                }

            } catch (Exception e) {

                e.printStackTrace();

                break;

            }

        }

    }

    public void clearStack() {

        getStack().clear();

    }

    public List<StackItem> getStack() {

        if(mStack == null){

            mStack = new ArrayList<>();

        }

        return mStack;

    }

    private void addToStack(boolean fragment) {

        getStack().add(new StackItem(mView , getRenderer().getForm().getModel() , fragment ? mFragment : null));

    }

    private StackItem popStack() {

        if (getStack().size() > 0) {

            return getStack().remove(getStack().size() - 1);

        }

        return null;

    }



}


class StackItem {
    public Model model;
    public String page;
    public Fragment fragment;
    public StackItem(String page ,Model model  ,Fragment fragment){
        this.model = model;
        this.page = page;
        this.fragment = fragment;
    }
}