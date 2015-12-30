/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.gearvrf.GVRMaterial.GVRShaderType;
import org.gearvrf.utility.Log;

/**
 * One of the key GVRF classes: a scene object.
 * 
 * Every scene object has a {@linkplain #getTransform() location}, and can have
 * {@linkplain #children() children}. An invisible scene object can be used to
 * move a set of scene as a unit, preserving their relative geometry. Invisible
 * scene objects don't need any {@linkplain GVRSceneObject#getRenderData()
 * render data.}
 * 
 * <p>
 * Visible scene objects must have render data
 * {@linkplain GVRSceneObject#attachRenderData(GVRRenderData) attached.} Each
 * {@link GVRRenderData} has a {@link GVRMesh GL mesh} that defines its
 * geometry, and a {@link GVRMaterial} that defines its surface.
 */
public class GVRSceneObject extends GVRHybridObject implements PrettyPrint {

    private GVRTransform mTransform;
    private GVRRenderData mRenderData;
    private GVRCamera mCamera;
    private GVRCameraRig mCameraRig;
    private GVREyePointeeHolder mEyePointeeHolder;
    private GVRSceneObject mParent;
    private final List<GVRSceneObject> mChildren = new ArrayList<GVRSceneObject>();

    /**
     * Constructs an empty scene object with a default {@link GVRTransform
     * transform}.
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     */
    public GVRSceneObject(GVRContext gvrContext) {
        super(gvrContext, NativeSceneObject.ctor());
        attachTransform(new GVRTransform(getGVRContext()));
    }

    /**
     * Constructs a scene object with an arbitrarily complex mesh.
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     * @param mesh
     *            a {@link GVRMesh} - usually generated by one of the
     *            {@link GVRContext#loadMesh(GVRAndroidResource)} methods, or
     *            {@link GVRContext#createQuad(float, float)}
     */
    public GVRSceneObject(GVRContext gvrContext, GVRMesh mesh) {
        this(gvrContext);
        GVRRenderData renderData = new GVRRenderData(gvrContext);
        attachRenderData(renderData);
        renderData.setMesh(mesh);
    }

    /**
     * Constructs a rectangular scene object, whose geometry is completely
     * specified by the width and height.
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     * @param width
     *            the scene object's width
     * @param height
     *            the scene object's height
     */
    public GVRSceneObject(GVRContext gvrContext, float width, float height) {
        this(gvrContext, gvrContext.createQuad(width, height));
    }

    /**
     * The base texture constructor: Constructs a scene object with
     * {@linkplain GVRMesh an arbitrarily complex geometry} that uses a specific
     * shader to display a {@linkplain GVRTexture texture.}
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     * @param mesh
     *            a {@link GVRMesh} - usually generated by one of the
     *            {@link GVRContext#loadMesh(GVRAndroidResource)} methods, or
     *            {@link GVRContext#createQuad(float, float)}
     * @param texture
     *            a {@link GVRTexture}
     * @param shaderId
     *            a specific shader Id - see {@link GVRShaderType} and
     *            {@link GVRMaterialShaderManager}
     * 
     */
    public GVRSceneObject(GVRContext gvrContext, GVRMesh mesh,
            GVRTexture texture, GVRMaterialShaderId shaderId) {
        this(gvrContext, mesh);

        GVRMaterial material = new GVRMaterial(gvrContext, shaderId);
        material.setMainTexture(texture);
        getRenderData().setMaterial(material);
    }

    private static final GVRMaterialShaderId STANDARD_SHADER = GVRShaderType.Texture.ID;

    /**
     * Constructs a scene object with {@linkplain GVRMesh an arbitrarily complex
     * geometry} that uses the standard {@linkplain Texture 'texture shader'} to
     * display a {@linkplain GVRTexture texture.}
     * 
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     * @param mesh
     *            a {@link GVRMesh} - usually generated by one of the
     *            {@link GVRContext#loadMesh(GVRAndroidResource)} methods, or
     *            {@link GVRContext#createQuad(float, float)}
     * @param texture
     *            a {@link GVRTexture}
     */
    public GVRSceneObject(GVRContext gvrContext, GVRMesh mesh,
            GVRTexture texture) {
        this(gvrContext, mesh, texture, STANDARD_SHADER);
    }

    /**
     * Very high-level constructor that asynchronously loads the mesh and
     * texture.
     * 
     * Note that because of <a href="package-summary.html#async">asynchronous
     * request consolidation</a> you generally don't have to do anything special
     * to create several objects that share the same mesh or texture: if you
     * create all the objects in {@link GVRScript#onInit(GVRContext) onInit(),}
     * the meshes and textures will generally <em>not</em> have loaded before
     * your {@code onInit()} method finishes. Thus, the loading code will see
     * that, say, {@code sceneObject2} and {@code sceneObject3} are using the
     * same mesh as {@code sceneObject1}, and will only load the mesh once.
     * 
     * @param gvrContext
     *            current {@link GVRContext}.
     * @param futureMesh
     *            mesh of the object.
     * @param futureTexture
     *            texture of the object.
     * 
     * @since 1.6.8
     */
    public GVRSceneObject(GVRContext gvrContext, Future<GVRMesh> futureMesh,
            Future<GVRTexture> futureTexture) {
        this(gvrContext);

        // Create the render data
        GVRRenderData renderData = new GVRRenderData(gvrContext);

        // Set the mesh
        renderData.setMesh(futureMesh);

        // Set the texture
        GVRMaterial material = new GVRMaterial(gvrContext);
        material.setMainTexture(futureTexture);
        renderData.setMaterial(material);

        // Attach the render data
        attachRenderData(renderData);
    }

    /**
     * Very high-level constructor that asynchronously loads the mesh and
     * texture.
     * 
     * @param gvrContext
     *            current {@link GVRContext}.
     * @param mesh
     *            Basically, a stream containing a mesh file.
     * @param texture
     *            Basically, a stream containing a texture file. This can be
     *            either a compressed texture or a regular Android bitmap file.
     * 
     * @since 1.6.7
     */
    public GVRSceneObject(GVRContext gvrContext, GVRAndroidResource mesh,
            GVRAndroidResource texture) {
        this(gvrContext, gvrContext.loadFutureMesh(mesh), gvrContext
                .loadFutureTexture(texture));
    }

    /**
     * Create a standard, rectangular texture object, using a non-default shader
     * to apply complex visual affects.
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     * @param width
     *            the rectangle's width
     * @param height
     *            the rectangle's height
     * @param texture
     *            a {@link GVRTexture}
     * @param shaderId
     *            a specific shader Id
     */
    public GVRSceneObject(GVRContext gvrContext, float width, float height,
            GVRTexture texture, GVRMaterialShaderId shaderId) {
        this(gvrContext, gvrContext.createQuad(width, height), texture,
                shaderId);
    }

    /**
     * Constructs a 2D, rectangular scene object that uses the standard
     * {@linkplain Texture 'texture shader'} to display a {@linkplain GVRTexture
     * texture.}
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     * @param width
     *            the rectangle's width
     * @param height
     *            the rectangle's height
     * @param texture
     *            a {@link GVRTexture}
     */
    public GVRSceneObject(GVRContext gvrContext, float width, float height,
            GVRTexture texture) {
        this(gvrContext, width, height, texture, STANDARD_SHADER);
    }

    /**
     * Get the (optional) name of the object.
     * 
     * @return The name of the object. If no name has been assigned, the
     *         returned string will be empty.
     */
    public String getName() {
        return NativeSceneObject.getName(getNative());
    }

    /**
     * Set the (optional) name of the object.
     * 
     * Scene object names are not needed: they are only for the application's
     * convenience.
     * 
     * @param name
     *            Name of the object.
     */
    public void setName(String name) {
        NativeSceneObject.setName(getNative(), name);
    }

    /**
     * Replace the current {@link GVRTransform transform}
     * 
     * @param transform
     *            New transform.
     */
    void attachTransform(GVRTransform transform) {
        mTransform = transform;
        NativeSceneObject.attachTransform(getNative(), transform.getNative());
    }

    /**
     * Remove the object's {@link GVRTransform transform}. After this call, the
     * object will have no transformations associated with it.
     */
    void detachTransform() {
        mTransform = null;
        NativeSceneObject.detachTransform(getNative());
    }

    /**
     * Get the {@link GVRTransform}.
     * 
     * A {@link GVRTransform} encapsulates a 4x4 matrix that specifies how to
     * render the {@linkplain GVRMesh GL mesh:} transform methods let you move,
     * rotate, and scale your scene object.
     * 
     * @return The current {@link GVRTransform transform}. If no transform is
     *         currently attached to the object, returns {@code null}.
     */
    public GVRTransform getTransform() {
        return mTransform;
    }

    /**
     * Attach {@linkplain GVRRenderData rendering data} to the object.
     * 
     * If other rendering data is currently attached, it is replaced with the
     * new data. {@link GVRRenderData} contains the GL mesh, the texture, the
     * shader id, and various shader constants.
     * 
     * @param renderData
     *            New rendering data.
     */
    public void attachRenderData(GVRRenderData renderData) {
        mRenderData = renderData;
        renderData.setOwnerObject(this);
        NativeSceneObject.attachRenderData(getNative(), renderData.getNative());
    }

    /**
     * Detach the object's current {@linkplain GVRRenderData rendering data}.
     * 
     * An object with no {@link GVRRenderData} is not visible.
     */
    public void detachRenderData() {
        if (mRenderData != null) {
            mRenderData.setOwnerObject(null);
        }
        mRenderData = null;
        NativeSceneObject.detachRenderData(getNative());
    }

    /**
     * Get the current {@link GVRRenderData}.
     * 
     * @return The current {@link GVRRenderData rendering data}. If no rendering
     *         data is currently attached to the object, returns {@code null}.
     */
    public GVRRenderData getRenderData() {
        return mRenderData;
    }

    /**
     * Attach a new {@link GVRCamera camera} to the object.
     * 
     * If another camera is currently attached, it is replaced with the new one.
     * 
     * @param camera
     *            New camera.
     */
    public void attachCamera(GVRCamera camera) {
        mCamera = camera;
        camera.setOwnerObject(this);
        NativeSceneObject.attachCamera(getNative(), camera.getNative());
    }

    /**
     * Detach the object's current {@link GVRCamera camera}.
     */
    public void detachCamera() {
        if (mCamera != null) {
            mCamera.setOwnerObject(null);
        }
        mCamera = null;
        NativeSceneObject.detachCamera(getNative());
    }

    /**
     * Get the {@link GVRCamera} attached to the object.
     * 
     * @return The {@link GVRCamera camera} attached to the object. If no camera
     *         is currently attached, returns {@code null}.
     */
    public GVRCamera getCamera() {
        return mCamera;
    }

    /**
     * Attach a new {@linkplain GVRCameraRig camera rig.}
     * 
     * If another camera rig is currently attached, it is replaced with the new
     * one.
     * 
     * @param cameraRig
     *            New camera rig.
     */
    public void attachCameraRig(GVRCameraRig cameraRig) {
        mCameraRig = cameraRig;
        cameraRig.setOwnerObject(this);
        NativeSceneObject.attachCameraRig(getNative(), cameraRig.getNative());
    }

    /**
     * Detach the object's current {@link GVRCameraRig camera rig}.
     */
    public void detachCameraRig() {
        if (mCameraRig != null) {
            mCameraRig.setOwnerObject(null);
        }
        mCameraRig = null;
        NativeSceneObject.detachCameraRig(getNative());
    }

    /**
     * Get the attached {@link GVRCameraRig}
     * 
     * @return The {@link GVRCameraRig camera rig} attached to the object. If no
     *         camera rig is currently attached, returns {@code null}.
     */
    public GVRCameraRig getCameraRig() {
        return mCameraRig;
    }

    /**
     * Attach a new {@link GVREyePointeeHolder} to the object.
     * 
     * If another {@link GVREyePointeeHolder} is currently attached, it is
     * replaced with the new one.
     * 
     * @param eyePointeeHolder
     *            New {@link GVREyePointeeHolder}.
     */
    public void attachEyePointeeHolder(GVREyePointeeHolder eyePointeeHolder) {
        // see GVRPicker.findObjects
        GVRPicker.sFindObjectsLock.lock();
        try {
            mEyePointeeHolder = eyePointeeHolder;
            eyePointeeHolder.setOwnerObject(this);
            NativeSceneObject.attachEyePointeeHolder(getNative(),
                eyePointeeHolder.getNative());
        } finally {
            GVRPicker.sFindObjectsLock.unlock();
        }
    }

    /**
     * Attach a default {@link GVREyePointeeHolder} to the object.
     * 
     * The default holder contains a single {@link GVRMeshEyePointee}, which
     * refers to the bounding box of the {@linkplain GVRMesh mesh} in this scene
     * object's {@linkplain GVRRenderData render data}. If you need more control
     * (multiple meshes, perhaps, or using the actual mesh instead of a bounding
     * box) use the {@linkplain #attachEyePointeeHolder(GVREyePointeeHolder)
     * explicit overload.} If another {@link GVREyePointeeHolder} is currently
     * attached, it is replaced with the new one.
     * 
     * @return {@code true} if and only this scene object has render data
     *         <em>and</em> you have called either
     *         {@link GVRRenderData#setMesh(GVRMesh)} or
     *         {@link GVRRenderData#setMesh(Future)}; {@code false}, otherwise.
     */
    public boolean attachEyePointeeHolder() {
        GVRRenderData renderData = getRenderData();
        if (renderData == null) {
            return false;
        }

        Future<GVREyePointee> eyePointee = renderData.getMeshEyePointee();
        if (eyePointee == null) {
            return false;
        }

        GVREyePointeeHolder eyePointeeHolder = new GVREyePointeeHolder(
                getGVRContext());
        eyePointeeHolder.addPointee(eyePointee);
        attachEyePointeeHolder(eyePointeeHolder);
        return true;
    }

    /**
     * Detach the object's current {@link GVREyePointeeHolder}.
     */
    public void detachEyePointeeHolder() {
        // see GVRPicker.findObjects
        GVRPicker.sFindObjectsLock.lock();
        try {
            if (mEyePointeeHolder != null) {
                mEyePointeeHolder.setOwnerObject(null);
            }
            mEyePointeeHolder = null;
            NativeSceneObject.detachEyePointeeHolder(getNative());
        } finally {
            GVRPicker.sFindObjectsLock.unlock();
        }
    }

    /**
     * Get the attached {@link GVREyePointeeHolder}
     * 
     * @return The {@link GVREyePointeeHolder} attached to the object. If no
     *         {@link GVREyePointeeHolder} is currently attached, returns
     *         {@code null}.
     */
    public GVREyePointeeHolder getEyePointeeHolder() {
        return mEyePointeeHolder;
    }

    /**
     * Simple, high-level API to enable or disable eye picking for this scene
     * object.
     * 
     * The {@linkplain #attachEyePointeeHolder(GVREyePointeeHolder) low-level
     * API} gives you a lot of control over eye picking, but it does involve an
     * awful lot of details. Since most apps are just going to use the
     * {@linkplain #attachEyePointeeHolder() simple API} anyhow, this method
     * (and {@link #getPickingEnabled()}) provides a simple boolean property.
     * 
     * @param enabled
     *            Should eye picking 'see' this scene object?
     * 
     * @since 2.0.2
     */
    public void setPickingEnabled(boolean enabled) {
        if (enabled != getPickingEnabled()) {
            if (enabled) {
                attachEyePointeeHolder();
            } else {
                detachEyePointeeHolder();
            }
        }
    }

    /**
     * Is eye picking enabled for this scene object?
     * 
     * @return Whether eye picking can 'see' this scene object?
     * 
     * @since 2.0.2
     */
    public boolean getPickingEnabled() {
        return mEyePointeeHolder != null;
    }

    /**
     * Get the {@linkplain GVRSceneObject parent object.}
     * 
     * If the object has been {@link #addChildObject(GVRSceneObject) added as a
     * child} to another {@link GVRSceneObject}, returns that object. Otherwise,
     * returns {@code null}.
     * 
     * @return The parent {@link GVRSceneObject} or {@code null}.
     */
    public GVRSceneObject getParent() {
        return mParent;
    }

    /**
     * Add {@code child} as a child of this object.
     * 
     * @param child
     *            {@link GVRSceneObject Object} to add as a child of this
     *            object.
     */
    public void addChildObject(GVRSceneObject child) {
        mChildren.add(child);
        child.mParent = this;
        NativeSceneObject.addChildObject(getNative(), child.getNative());
    }

    /**
     * Remove {@code child} as a child of this object.
     * 
     * @param child
     *            {@link GVRSceneObject Object} to remove as a child of this
     *            object.
     */
    public void removeChildObject(GVRSceneObject child) {
        mChildren.remove(child);
        child.mParent = null;
        NativeSceneObject.removeChildObject(getNative(), child.getNative());
    }

    /**
     * Performs case-sensitive search
     * 
     * @param name
     * @return null if nothing was found or name was null/empty
     */
    public GVRSceneObject[] getSceneObjectsByName(final String name) {
        if (null == name || name.isEmpty()) {
            return null;
        }

        final List<GVRSceneObject> matches = new ArrayList<GVRSceneObject>();
        if (name.equals(getName())) {
            matches.add(this);
        }
        GVRScene.getSceneObjectsByName(matches, mChildren, name);

        return 0 != matches.size() ? matches.toArray(new GVRSceneObject[matches.size()]) : null;
    }

    /**
     * Performs case-sensitive depth-first search
     * 
     * @param name
     * @return first match in the graph; null if nothing was found or name was null/empty;
     * in case there might be multiple matches consider using getSceneObjectsByName
     */
    public GVRSceneObject getSceneObjectByName(final String name) {
        if (null == name || name.isEmpty()) {
            return null;
        }

        GVRSceneObject scene = GVRScene.getSceneObjectByName(mChildren, name);
        if (null == scene && name.equals(getName())) {
            scene = this;
        }
        return scene;
    }

    /**
     * Check if {@code otherObject} is colliding with this object.
     * 
     * @param otherObject
     *            {@link GVRSceneObject Object} to check for collision with this
     *            object.
     * @return {@code true) if objects collide, {@code false} otherwise
     */
    public boolean isColliding(GVRSceneObject otherObject) {
        return NativeSceneObject.isColliding(getNative(),
                otherObject.getNative());
    }

    /**
     * Sets the range of distances from the camera where this object will be shown.
     *
     * @param minRange
     *      The closest distance to the camera rig in which this object should be shown.  This should be a positive number between 0 and Float.MAX_VALUE.
     * @param maxRange
     *      The farthest distance to the camera rig in which this object should be shown.  This should be a positive number between 0 and Float.MAX_VALUE.
     */
    public void setLODRange(float minRange, float maxRange) {
        if (minRange < 0 || maxRange < 0) {
            throw new IllegalArgumentException(
                    "minRange and maxRange must be between 0 and Float.MAX_VALUE");
        }
        if (minRange > maxRange) {
            throw new IllegalArgumentException(
                    "minRange should not be greater than maxRange");
        }
        NativeSceneObject.setLODRange(getNative(), minRange, maxRange);
    }

    /**
     * Get the minimum distance from the camera in which to show this object.
     * 
     * @return the minimum distance from the camera in which to show this object.  Default value is 0.
     */
    public float getLODMinRange() {
        return NativeSceneObject.getLODMinRange(getNative());
    }

    /**
     * Get the maximum distance from the camera in which to show this object.
     * 
     * @return the maximum distance from the camera in which to show this object.  Default value is Float.MAX_VALUE.
     */
    public float getLODMaxRange() {
        return NativeSceneObject.getLODMaxRange(getNative());
    }

    /**
     * Get the number of child objects.
     * 
     * @return Number of {@link GVRSceneObject objects} added as children of
     *         this object.
     */
    public int getChildrenCount() {
        return mChildren.size();
    }

    /**
     * Get the child object at {@code index}.
     * 
     * @param index
     *            Position of the child to get.
     * @return {@link GVRSceneObject Child object}.
     * 
     * @throws {@link java.lang.IndexOutOfBoundsException} if there is no child
     *         at that position.
     */
    public GVRSceneObject getChildByIndex(int index) {
        return mChildren.get(index);
    }

    /**
     * As an alternative to calling {@link #getChildrenCount()} then repeatedly
     * calling {@link #getChildByIndex(int)}, you can
     * 
     * <pre>
     * for (GVRSceneObject child : parent.children()) {
     * }
     * </pre>
     * 
     * @return An {@link Iterable}, so you can use Java's enhanced for loop.
     *         This {@code Iterable} gives you an {@link Iterator} that does not
     *         support {@link Iterator#remove()}.
     *         <p>
     *         At some point, this might actually return a
     *         {@code List<GVRSceneObject>}, but that would require either
     *         creating an immutable copy or writing a lot of code to support
     *         methods like {@link List#addAll(java.util.Collection)} and
     *         {@link List#clear()} - for now, we just create a very
     *         light-weight class that only supports iteration.
     */
    public Iterable<GVRSceneObject> children() {
        return new Children(this);
    }

    /**
     * Get all the children, in a single list.
     * 
     * @return An un-modifiable list of this object's children.
     * 
     * @since 2.0.0
     */
    public List<GVRSceneObject> getChildren() {
        return Collections.unmodifiableList(mChildren);
    }

    /** The internal list - do not make any changes! */
    List<GVRSceneObject> rawGetChildren() {
        return mChildren;
    }

    private static class Children implements Iterable<GVRSceneObject>,
            Iterator<GVRSceneObject> {

        private final GVRSceneObject object;
        private int index;

        private Children(GVRSceneObject object) {
            this.object = object;
            this.index = 0;
        }

        @Override
        public Iterator<GVRSceneObject> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return index < object.getChildrenCount();
        }

        @Override
        public GVRSceneObject next() {
            return object.getChildByIndex(index++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Add {@code childComponent} as a child of this object (owner object of the
     * component is added as child). Adding a component will increase the
     * {@link getChildrenCount() getChildrenCount()} for this scene object.
     * 
     * @param childComponent
     *            {@link GVRComponent Component} to add as a child of this
     *            object.
     */
    public void addChildObject(GVRComponent childComponent) {
        if (childComponent.getOwnerObject() != null) {
            addChildObject(childComponent.getOwnerObject());
        }
    }

    /**
     * Remove {@code childComponent} as a child of this object (owner object of
     * the component is removed as child). Removing a component will decrease
     * the {@link getChildrenCount() getChildrenCount()} for this scene object.
     * 
     * @param childComponent
     *            {@link GVRComponent Component} to remove as a child of this
     *            object.
     */
    public void removeChildObject(GVRComponent childComponent) {
        if (childComponent.getOwnerObject() != null) {
            removeChildObject(childComponent.getOwnerObject());
        }
    }

    /**
     * Called when the scene object has been loaded from a model.
     */
    public void onLoaded() {
    }

    /**
     * Generate debug dump of the tree from the scene object.
     * It should include a newline character at the end.
     * 
     * @param sb the {@code StringBuffer} to dump the object.
     * @param indent indentation level as number of spaces.
     */
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(getClass().getSimpleName());
        sb.append(" [name=");
        sb.append(this.getName());
        sb.append("]");
        sb.append(System.lineSeparator());

        if (mRenderData == null) {
            sb.append(Log.getSpaces(indent + 2));
            sb.append("RenderData: null");
            sb.append(System.lineSeparator());
        } else {
            mRenderData.prettyPrint(sb, indent + 2);
        }
        sb.append(Log.getSpaces(indent + 2));
        sb.append("Transform: "); sb.append(mTransform);
        sb.append(System.lineSeparator());

        // dump its children
        for (GVRSceneObject child : getChildren()) {
            child.prettyPrint(sb, indent + 2);
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }
}

class NativeSceneObject {
    static native long ctor();

    static native String getName(long sceneObject);

    static native void setName(long sceneObject, String name);

    static native void attachTransform(long sceneObject, long transform);

    static native void detachTransform(long sceneObject);

    static native void attachRenderData(long sceneObject, long renderData);

    static native void detachRenderData(long sceneObject);

    static native void attachCamera(long sceneObject, long camera);

    static native void detachCamera(long sceneObject);

    static native void attachCameraRig(long sceneObject, long cameraRig);

    static native void detachCameraRig(long sceneObject);

    static native void attachEyePointeeHolder(long sceneObject,
            long eyePointeeHolder);

    static native void detachEyePointeeHolder(long sceneObject);

    static native long setParent(long sceneObject, long parent);

    static native void addChildObject(long sceneObject, long child);

    static native void removeChildObject(long sceneObject, long child);

    static native boolean isColliding(long sceneObject, long otherObject);

    static native void setLODRange(long sceneObject, float minRange, float maxRange);
    static native float getLODMinRange(long sceneObject);
    static native float getLODMaxRange(long sceneObject);
}
