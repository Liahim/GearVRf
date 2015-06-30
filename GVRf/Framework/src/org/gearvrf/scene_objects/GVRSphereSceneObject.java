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

package org.gearvrf.scene_objects;

import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMesh;
import org.gearvrf.utility.Log;

public class GVRSphereSceneObject extends GVRSceneObject {

    @SuppressWarnings("unused")
    private static final String TAG = Log.tag(GVRSphereSceneObject.class);

    private static final int STACK_NUMBER = 18;
    private static final int SLICE_NUMBER = 36;

    private float[] vertices;
    private float[] normals;
    private float[] texCoords;
    private char[] indices;

    private int vertexCount = 0;
    private int texCoordCount = 0;
    private char indexCount = 0;
    private char triangleCount = 0;

    /**
     * Constructs a sphere scene object with a radius of 1 and 18 stacks, and 36
     * slices.
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     */
    public GVRSphereSceneObject(GVRContext gvrContext) {
        super(gvrContext);

        generateSceneObject(gvrContext, STACK_NUMBER, SLICE_NUMBER);
    }

    /**
     * Constructs a sphere scene object with a radius of 1 and 18 stacks, and 36
     * slices.
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     * @param stackNumber
     *            The number of rows high.
     * @param sliceNumber
     *            The number of slices around the sphere.
     */
    public GVRSphereSceneObject(GVRContext gvrContext, int stackNumber, int sliceNumber) {
        super(gvrContext);

        // assert numStacks, numSlices > 0
        if (stackNumber <= 0 || sliceNumber <= 0) {
            throw new IllegalArgumentException(
                    "numStacks, and numSlices must be > 0.  Values passed were: numStacks=" + stackNumber + ", numSlices=" + sliceNumber);
        }


        generateSceneObject(gvrContext, stackNumber, sliceNumber);
    }

    private void generateSceneObject(GVRContext gvrContext, int stackNumber, int sliceNumber) {
        generateSphere(stackNumber, sliceNumber);

        GVRMesh mesh = new GVRMesh(gvrContext);
        mesh.setVertices(vertices);
        mesh.setNormals(normals);
        mesh.setTexCoords(texCoords);
        mesh.setTriangles(indices);

        GVRRenderData renderData = new GVRRenderData(gvrContext);
        attachRenderData(renderData);
        renderData.setMesh(mesh);
    }

    private void generateSphere(int stackNumber, int sliceNumber) {
        int capVertexNumber = 3 * sliceNumber;
        int bodyVertexNumber = 4 * sliceNumber * stackNumber;
        int vertexNumber = (2 * capVertexNumber) + bodyVertexNumber;
        int triangleNumber = (2 * capVertexNumber)
                + (6 * sliceNumber * stackNumber);

        vertices = new float[3 * vertexNumber];
        normals = new float[3 * vertexNumber];
        texCoords = new float[2 * vertexNumber];
        indices = new char[triangleNumber];

        // bottom cap
        createCap(0, stackNumber, sliceNumber, false);

        // body
        createBody(stackNumber, sliceNumber);

        // top cap
        createCap(stackNumber, stackNumber, sliceNumber, true);
    }

    private void createCap(int stack, int stackNumber, int sliceNumber,
            boolean top) {

        float stackPercentage0;
        float stackPercentage1;

        if (top) {
            stackPercentage0 = ((float) (stack - 1) / stackNumber);
            stackPercentage1 = ((float) (stack) / stackNumber);

        } else {
            stackPercentage0 = ((float) (stack + 1) / stackNumber);
            stackPercentage1 = ((float) (stack) / stackNumber);
        }

        float t0 = stackPercentage0;
        float t1 = stackPercentage1;
        double theta1 = stackPercentage0 * Math.PI;
        double theta2 = stackPercentage1 * Math.PI;
        double cosTheta1 = Math.cos(theta1);
        double sinTheta1 = Math.sin(theta1);
        double cosTheta2 = Math.cos(theta2);
        double sinTheta2 = Math.sin(theta2);

        for (int slice = 0; slice < sliceNumber; slice++) {
            float slicePercentage0 = ((float) (slice) / sliceNumber);
            float slicePercentage1 = ((float) (slice + 1) / sliceNumber);
            double phi1 = slicePercentage0 * 2.0 * Math.PI;
            double phi2 = slicePercentage1 * 2.0 * Math.PI;
            float s0 = slicePercentage0;
            float s1 = slicePercentage1;
            float s2 = (s0 + s1) / 2.0f;
            double cosPhi1 = Math.cos(phi1);
            double sinPhi1 = Math.sin(phi1);
            double cosPhi2 = Math.cos(phi2);
            double sinPhi2 = Math.sin(phi2);

            float x0 = (float) (sinTheta1 * cosPhi1);
            float y0 = (float) (sinTheta1 * sinPhi1);
            float z0 = (float) cosTheta1;

            float x1 = (float) (sinTheta1 * cosPhi2);
            float y1 = (float) (sinTheta1 * sinPhi2);
            float z1 = (float) cosTheta1;

            float x2 = (float) (sinTheta2 * cosPhi1);
            float y2 = (float) (sinTheta2 * sinPhi1);
            float z2 = (float) cosTheta2;

            vertices[vertexCount + 0] = x0;
            vertices[vertexCount + 1] = y0;
            vertices[vertexCount + 2] = z0;

            vertices[vertexCount + 3] = x1;
            vertices[vertexCount + 4] = y1;
            vertices[vertexCount + 5] = z1;

            vertices[vertexCount + 6] = x2;
            vertices[vertexCount + 7] = y2;
            vertices[vertexCount + 8] = z2;

            normals[vertexCount + 0] = x0;
            normals[vertexCount + 1] = y0;
            normals[vertexCount + 2] = z0;

            normals[vertexCount + 3] = x1;
            normals[vertexCount + 4] = y1;
            normals[vertexCount + 5] = z1;

            normals[vertexCount + 6] = x2;
            normals[vertexCount + 7] = y2;
            normals[vertexCount + 8] = z2;

            texCoords[texCoordCount + 0] = s0;
            texCoords[texCoordCount + 1] = t0;
            texCoords[texCoordCount + 2] = s1;
            texCoords[texCoordCount + 3] = t0;
            texCoords[texCoordCount + 4] = s2;
            texCoords[texCoordCount + 5] = t1;

            if (top) {
                indices[indexCount + 0] = (char) (triangleCount + 1);
                indices[indexCount + 1] = (char) (triangleCount + 0);
                indices[indexCount + 2] = (char) (triangleCount + 2);
            } else {
                indices[indexCount + 0] = (char) (triangleCount + 0);
                indices[indexCount + 1] = (char) (triangleCount + 1);
                indices[indexCount + 2] = (char) (triangleCount + 2);
            }

            vertexCount += 9;
            texCoordCount += 6;
            indexCount += 3;
            triangleCount += 3;
        }

    }

    private void createBody(int stackNumber, int sliceNumber) {
        for (int stack = 1; stack < stackNumber - 1; stack++) {
            float stackPercentage0 = ((float) (stack) / stackNumber);
            float stackPercentage1 = ((float) (stack + 1) / stackNumber);

            float t0 = stackPercentage0;
            float t1 = stackPercentage1;
            
            double theta1 = stackPercentage0 * Math.PI;
            double theta2 = stackPercentage1 * Math.PI;
            double cosTheta1 = Math.cos(theta1);
            double sinTheta1 = Math.sin(theta1);
            double cosTheta2 = Math.cos(theta2);
            double sinTheta2 = Math.sin(theta2);

            for (int slice = 0; slice < sliceNumber; slice++) {
                float slicePercentage0 = ((float) (slice) / sliceNumber);
                float slicePercentage1 = ((float) (slice + 1) / sliceNumber);
                double phi1 = slicePercentage0 * 2.0 * Math.PI;
                double phi2 = slicePercentage1 * 2.0 * Math.PI;
                float s0 = slicePercentage0;
                float s1 = slicePercentage1;
                double cosPhi1 = Math.cos(phi1);
                double sinPhi1 = Math.sin(phi1);
                double cosPhi2 = Math.cos(phi2);
                double sinPhi2 = Math.sin(phi2);

                float x0 = (float) (sinTheta1 * cosPhi1);
                float y0 = (float) (sinTheta1 * sinPhi1);
                float z0 = (float) cosTheta1;

                float x1 = (float) (sinTheta1 * cosPhi2);
                float y1 = (float) (sinTheta1 * sinPhi2);
                float z1 = (float) cosTheta1;

                float x2 = (float) (sinTheta2 * cosPhi1);
                float y2 = (float) (sinTheta2 * sinPhi1);
                float z2 = (float) cosTheta2;

                float x3 = (float) (sinTheta2 * cosPhi2);
                float y3 = (float) (sinTheta2 * sinPhi2);
                float z3 = (float) cosTheta2;

                vertices[vertexCount + 0] = x0;
                vertices[vertexCount + 1] = y0;
                vertices[vertexCount + 2] = z0;

                vertices[vertexCount + 3] = x1;
                vertices[vertexCount + 4] = y1;
                vertices[vertexCount + 5] = z1;

                vertices[vertexCount + 6] = x2;
                vertices[vertexCount + 7] = y2;
                vertices[vertexCount + 8] = z2;

                vertices[vertexCount + 9] = x3;
                vertices[vertexCount + 10] = y3;
                vertices[vertexCount + 11] = z3;

                normals[vertexCount + 0] = x0;
                normals[vertexCount + 1] = y0;
                normals[vertexCount + 2] = z0;

                normals[vertexCount + 3] = x1;
                normals[vertexCount + 4] = y1;
                normals[vertexCount + 5] = z1;

                normals[vertexCount + 6] = x2;
                normals[vertexCount + 7] = y2;
                normals[vertexCount + 8] = z2;

                normals[vertexCount + 9] = x3;
                normals[vertexCount + 10] = y3;
                normals[vertexCount + 11] = z3;

                texCoords[texCoordCount + 0] = s0;
                texCoords[texCoordCount + 1] = t0;
                texCoords[texCoordCount + 2] = s1;
                texCoords[texCoordCount + 3] = t0;
                texCoords[texCoordCount + 4] = s0;
                texCoords[texCoordCount + 5] = t1;
                texCoords[texCoordCount + 6] = s1;
                texCoords[texCoordCount + 7] = t1;

                // 0, 2, 1
                // 2, 3, 1
                indices[indexCount + 0] = (char) (triangleCount + 0);
                indices[indexCount + 1] = (char) (triangleCount + 2);
                indices[indexCount + 2] = (char) (triangleCount + 1);
                indices[indexCount + 3] = (char) (triangleCount + 2);
                indices[indexCount + 4] = (char) (triangleCount + 3);
                indices[indexCount + 5] = (char) (triangleCount + 1);

                vertexCount += 12;
                texCoordCount += 8;
                indexCount += 6;
                triangleCount += 4;
            }
        }

    }

}
